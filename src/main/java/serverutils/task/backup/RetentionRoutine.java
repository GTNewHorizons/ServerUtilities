package serverutils.task.backup;

import static serverutils.ServerUtilitiesConfig.backups;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import serverutils.ServerUtilities;
import serverutils.lib.util.CancellationToken;
import serverutils.lib.util.FileUtils;
import serverutils.task.backup.repository.Repository;

public class RetentionRoutine {

    private final CancellationToken cancellation = new CancellationToken();
    /** Each store is pruned on its own, with its own size allowance. */
    private final List<BackupStore<?>> stores;
    private final Options options;

    public RetentionRoutine(BackupStore<?> store, Options options) {
        this(Collections.singletonList(store), options);
    }

    public RetentionRoutine(List<BackupStore<?>> stores, Options options) {
        this.stores = stores;
        this.options = options;
    }

    /** The ZIP backups, and the incremental repository once a backup has created it. */
    public static RetentionRoutine fromConfig() {
        List<BackupStore<?>> stores = new ArrayList<>();
        stores.add(new ArchiveStore(BackupRoutine.getDefaultBackupFolder()));
        File repository = BackupRoutine.getRepositoryFolder();
        if (Repository.exists(repository)) stores.add(new RepositoryStore(repository));
        return new RetentionRoutine(stores, Options.fromConfig());
    }

    /** Deletes archive staging files left by an interrupted backup. Only call while no backup can be running. */
    public static void deleteAbandonedArchives(File folder) {
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (!isArchiveStagingFile(file) || !Files.isRegularFile(file.toPath(), LinkOption.NOFOLLOW_LINKS)) continue;
            try {
                Files.delete(file.toPath());
            } catch (IOException ex) {
                ServerUtilities.LOGGER.warn("Could not delete abandoned backup staging file {}", file, ex);
            }
        }
    }

    private static boolean isArchiveStagingFile(File file) {
        return file.getName().startsWith(".su-save-") && file.getName().endsWith(".tmp");
    }

    public BackupResult.Retention prune() {
        BackupResult.Retention result = new BackupResult.Retention();
        cancellation.enable();
        try {
            for (BackupStore<?> store : stores) {
                try {
                    try {
                        prune(store, result);
                    } catch (UncheckedIOException ex) {
                        throw ex.getCause();
                    }
                } catch (InterruptedIOException ex) {
                    ServerUtilities.LOGGER.info("Backup retention cancelled");
                    result.error = ex;
                    break;
                } catch (IOException | IllegalArgumentException | ArithmeticException ex) {
                    // A locked repository must not keep the ZIP backups from being pruned, or the other way round.
                    ServerUtilities.LOGGER.warn("Skipping backup pruning: {}", ex.getMessage());
                    result.error = ex;
                }
            }
        } finally {
            cancellation.disable();
            close();
        }
        return result.finished();
    }

    /** One plan per store, or null when legacy retention is active. */
    public List<BackupRetention.Plan<? extends BackupEntry>> preview() throws IOException {
        if (!options.hasPolicy()) return null;
        try {
            cancellation.enable();
            List<BackupRetention.Plan<? extends BackupEntry>> plans = new ArrayList<>();
            for (BackupStore<?> store : stores) plans.add(getRetentionPlan(store));
            return plans;
        } catch (UncheckedIOException ex) {
            throw ex.getCause();
        } finally {
            cancellation.disable();
            close();
        }
    }

    public List<? extends BackupEntry> list() throws IOException {
        try {
            cancellation.enable();
            List<BackupEntry> entries = new ArrayList<>();
            for (BackupStore<?> store : stores) entries.addAll(store.list());
            return entries;
        } finally {
            cancellation.disable();
            close();
        }
    }

    private void close() {
        for (BackupStore<?> store : stores) {
            if (!(store instanceof Closeable closeable)) continue;
            try {
                closeable.close();
            } catch (IOException ex) {
                ServerUtilities.LOGGER.warn("Could not close backup store", ex);
            }
        }
    }

    public void cancel() {
        cancellation.cancel();
    }

    private <T extends BackupEntry> void prune(BackupStore<T> store, BackupResult.Retention result) throws IOException {
        if (options.hasPolicy()) {
            pruneByPolicy(store, result);
        } else {
            pruneBySize(store, result);
        }
        store.reclaim();
    }

    private <T extends BackupEntry> void pruneByPolicy(BackupStore<T> store, BackupResult.Retention result)
            throws IOException {
        BackupRetention.Plan<T> plan = getRetentionPlan(store);
        long maxSize = options.maxSize;

        List<T> counted = new ArrayList<>();
        List<T> exempt = new ArrayList<>();
        for (T entry : plan.keep.keySet()) {
            (plan.sizeExempt.contains(entry) ? exempt : counted).add(entry);
        }
        counted.addAll(plan.delete.keySet());
        long remaining = store.countedSize(counted, exempt);

        // Rule deletions go in one batch, so stores with shared data reclaim it once.
        for (Map.Entry<T, String> decision : plan.delete.entrySet()) {
            ServerUtilities.LOGGER.info("Removing old backup {} ({})", decision.getKey(), decision.getValue());
        }
        BackupRetention.checkInterrupted();
        remaining -= delete(store, plan.delete.keySet(), result);

        for (T entry : plan.sizeCandidates) {
            BackupRetention.checkInterrupted();
            if (maxSize <= 0 || remaining <= maxSize) break;
            if (plan.delete.containsKey(entry)) continue;
            ServerUtilities.LOGGER.info("Removing old backup {} (Folder size limit)", entry);
            remaining -= delete(store, Collections.singleton(entry), result);
        }

        if (maxSize > 0 && remaining > maxSize) {
            ServerUtilities.LOGGER.warn(
                    "Backup rotation size allowance could not be met: {} counted bytes remain; protected backups or failed deletions",
                    remaining);
        }
        for (T entry : plan.keep.keySet()) {
            if (entry.problem() != null) {
                ServerUtilities.LOGGER.warn("Preserving backup {}: {}", entry.toString(), entry.problem());
            }
        }
    }

    private <T extends BackupEntry> void pruneBySize(BackupStore<T> store, BackupResult.Retention result)
            throws IOException {
        long maxSize = options.maxSize;
        if (maxSize < 0) {
            ServerUtilities.LOGGER.warn("Skipping legacy backup pruning: negative size limit");
            return;
        }
        int keepCount = Math.max(1, options.keepCount);

        List<T> preserved = new ArrayList<>();
        List<T> entries = readLegacyBackups(store, preserved);
        Set<T> newest = newestPerWorld(entries);
        long currentSize = store.countedSize(entries, preserved);
        int remainingCount = entries.size();

        for (T entry : entries) {
            BackupRetention.checkInterrupted();
            if (maxSize > 0 ? currentSize <= maxSize : remainingCount <= keepCount) break;
            if (newest.contains(entry)) continue;
            ServerUtilities.LOGGER.info("Removing old backup {} (legacy rotation)", entry.toString());
            int removedBefore = result.filesRemoved;
            long freed = delete(store, Collections.singleton(entry), result);
            if (result.filesRemoved > removedBefore) {
                currentSize -= freed;
                remainingCount--;
            }
        }

        if (maxSize > 0 && currentSize > maxSize) {
            ServerUtilities.LOGGER.warn(
                    "Legacy backup size limit could not be met: {} bytes remain; latest backups per world or failed deletions",
                    currentSize);
        } else if (maxSize == 0 && remainingCount > keepCount) {
            ServerUtilities.LOGGER
                    .warn("Legacy backup count limit could not be met: {} backups remain", remainingCount);
        }
    }

    /** Deletes through the store and returns the bytes it freed. */
    private static <T extends BackupEntry> long delete(BackupStore<T> store, Collection<T> entries,
            BackupResult.Retention result) throws IOException {
        if (entries.isEmpty()) return 0;
        long before = result.bytesRemoved;
        store.delete(entries, result);
        return result.bytesRemoved - before;
    }

    /** The most recent entry of each world, which legacy rotation never deletes. Expects oldest-first order. */
    private static <T extends BackupEntry> Set<T> newestPerWorld(List<T> entries) {
        Set<String> worlds = new HashSet<>();
        Set<T> newest = new HashSet<>();
        for (int i = entries.size() - 1; i >= 0; i--) {
            T entry = entries.get(i);
            if (worlds.add(entry.world())) newest.add(entry);
        }
        return newest;
    }

    /** The entries legacy rotation may delete, oldest first. The rest are added to {@code preserved}. */
    private <T extends BackupEntry> List<T> readLegacyBackups(BackupStore<T> store, List<T> preserved)
            throws IOException {
        List<T> entries = new ArrayList<>(store.list());
        entries.removeIf(entry -> isPreserved(entry) && preserved.add(entry));

        entries.sort(Comparator.comparingLong((T entry) -> entry.createdAt()).thenComparing(BackupEntry::label));

        BackupRetention.checkInterrupted();
        return entries;
    }

    private boolean isPreserved(BackupEntry entry) {
        if (entry.createdAt() > System.currentTimeMillis()) {
            ServerUtilities.LOGGER.warn("Preserving future-dated backup {}", entry.toString());
            return true;
        }

        if (entry.problem() != null) {
            ServerUtilities.LOGGER.warn("Preserving backup with problem {}: {}", entry.toString(), entry.problem());
            return true;
        }

        return entry.isCustom() && !options.deleteCustom;
    }

    private <T extends BackupEntry> BackupRetention.Plan<T> getRetentionPlan(BackupStore<T> store) throws IOException {
        return BackupRetention.plan(store, options.policy, options.deleteCustom, options.maxSize);
    }

    public static class Options {

        public List<String> policy;
        public boolean deleteCustom;
        public long maxSize;
        public int keepCount;

        public Options(List<String> policy, boolean deleteCustom, long maxSize, int keepCount) {
            this.policy = policy;
            this.deleteCustom = deleteCustom;
            this.maxSize = maxSize * FileUtils.SizeUnit.GB.getSize();
            this.keepCount = keepCount;
        }

        public static Options fromConfig() {
            return new Options(
                    Collections.unmodifiableList(Arrays.asList(backups.retention_policy)),
                    backups.delete_custom_name_backups,
                    backups.max_folder_size,
                    backups.backups_to_keep);
        }

        public boolean hasPolicy() {
            return policy != null && !policy.isEmpty();
        }
    }
}
