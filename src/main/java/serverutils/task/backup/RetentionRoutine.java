package serverutils.task.backup;

import static serverutils.ServerUtilitiesConfig.backups;

import java.io.IOException;
import java.io.InterruptedIOException;
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

public class RetentionRoutine {

    private final CancellationToken cancellation = new CancellationToken();
    private final BackupStore<?> store;
    private final Options options;

    public RetentionRoutine(BackupStore<?> store, Options options) {
        this.store = store;
        this.options = options;
    }

    public static RetentionRoutine fromConfig() {
        return new RetentionRoutine(new ArchiveStore(BackupRoutine.BACKUP_FOLDER), Options.fromConfig());
    }

    public BackupResult.Retention prune() {
        BackupResult.Retention result = new BackupResult.Retention();
        cancellation.enable();
        try {
            prune(store, result);
        } catch (InterruptedIOException ex) {
            ServerUtilities.LOGGER.info("Backup retention cancelled");
            result.error = ex;
        } catch (IOException | IllegalArgumentException | ArithmeticException ex) {
            ServerUtilities.LOGGER.warn("Skipping backup pruning: {}", ex.getMessage());
            result.error = ex;
        } finally {
            cancellation.disable();
        }
        return result.finished();
    }

    public BackupRetention.Plan<? extends BackupEntry> preview() throws IOException {
        if (!options.hasPolicy()) return null;
        try {
            cancellation.enable();
            return getRetentionPlan(store);
        } finally {
            cancellation.disable();
        }
    }

    public List<? extends BackupEntry> list() throws IOException {
        try {
            cancellation.enable();
            return store.list();
        } finally {
            cancellation.disable();
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
    }

    private <T extends BackupEntry> void pruneByPolicy(BackupStore<T> store, BackupResult.Retention result)
            throws IOException {
        BackupRetention.Plan<T> plan = getRetentionPlan(store);
        long maxSize = options.maxSize;

        long remaining = 0;
        for (T entry : plan.keep.keySet()) {
            if (!plan.sizeExempt.contains(entry)) remaining += entry.size();
        }
        for (T entry : plan.delete.keySet()) remaining += entry.size();

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

        List<T> entries = readLegacyBackups(store);
        Set<T> newest = newestPerWorld(entries);
        long currentSize = entries.stream().mapToLong(BackupEntry::size).sum();
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

    private <T extends BackupEntry> List<T> readLegacyBackups(BackupStore<T> store) throws IOException {
        List<T> entries = new ArrayList<>(store.list());
        entries.removeIf(entry -> {
            if (entry.createdAt() > System.currentTimeMillis()) {
                ServerUtilities.LOGGER.warn("Preserving future-dated backup {}", entry.toString());
                return true;
            }

            if (entry.problem() != null) {
                ServerUtilities.LOGGER.warn("Preserving backup with problem {}: {}", entry.id(), entry.problem());
                return true;
            }

            return entry.isCustom() && !options.deleteCustom;
        });

        entries.sort(Comparator.comparingLong((T entry) -> entry.createdAt()).thenComparing(BackupEntry::id));

        BackupRetention.checkInterrupted();
        return entries;
    }

    private <T extends BackupEntry> BackupRetention.Plan<T> getRetentionPlan(BackupStore<T> store) throws IOException {
        return BackupRetention.plan(store.list(), options.policy, options.deleteCustom, options.maxSize);
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
