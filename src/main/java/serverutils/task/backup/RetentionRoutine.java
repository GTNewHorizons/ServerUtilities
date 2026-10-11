package serverutils.task.backup;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import serverutils.ServerUtilities;
import serverutils.lib.util.CancellationToken;
import serverutils.lib.util.FileUtils;


import static serverutils.ServerUtilitiesConfig.backups;

public class RetentionRoutine {
    private final CancellationToken cancellation = new CancellationToken();
    private final Options options;

    public RetentionRoutine(Options options) {
        this.options = options;
    }

    public static RetentionRoutine fromConfig() {
        return new RetentionRoutine(Options.fromConfig());
    }

    public void prune()  {
        cancellation.enable();
        try {
            if (options.hasPolicy()) {
                pruneByPolicy();
            } else {
                pruneBySize();
            }
        } catch (InterruptedIOException ex) {
            ServerUtilities.LOGGER.info("Backup retention cancelled");
        } catch (IOException | IllegalArgumentException | ArithmeticException ex) {
            ServerUtilities.LOGGER.warn("Skipping backup pruning: {}", ex.getMessage());
        } finally {
            cancellation.disable();
        }
    }

    public BackupRetention.Plan preview() throws IOException {
        if (!options.hasPolicy()) return null;
        try {
            cancellation.enable();
            return getRetentionPlan();
        } finally {
            cancellation.disable();
        }
    }

    public List<BackupRetention.Archive> list() throws IOException {
        try {
            cancellation.enable();
            return BackupRetention.readArchives(options.backupFolder);
        } finally {
            cancellation.disable();
        }
    }

    public void cancel() {
        cancellation.cancel();
    }

    private void pruneByPolicy() throws IOException {
        BackupRetention.Plan plan = getRetentionPlan();
        long maxSize = options.maxSize;

        long remaining = 0;
        for (File file : plan.keep.keySet()) {
            if (!plan.sizeExempt.contains(file)) remaining += file.length();
        }
        for (File file : plan.delete.keySet()) remaining += file.length();

        Map<File, String> candidates = new LinkedHashMap<>(plan.delete);
        for (File file : plan.sizeCandidates) candidates.putIfAbsent(file, "Folder size limit");

        for (Map.Entry<File, String> candidate : candidates.entrySet()) {
            BackupRetention.checkInterrupted();
            File file = candidate.getKey();
            boolean mandatory = plan.delete.containsKey(file);
            if (!mandatory && (maxSize <= 0 || remaining <= maxSize)) continue;
            long freed = delete(file, candidate.getValue());
            if (freed >= 0) remaining -= freed;
        }

        if (maxSize > 0 && remaining > maxSize) {
            ServerUtilities.LOGGER.warn(
                "Backup rotation size allowance could not be met: {} counted bytes remain; protected backups or failed deletions",
                remaining);
        }
        for (Map.Entry<File, String> decision : plan.keep.entrySet()) {
            if (decision.getValue().startsWith("Unrecognized/unreadable")) {
                ServerUtilities.LOGGER.warn("Preserving backup {}: {}", decision.getKey(), decision.getValue());
            }
        }
    }

    private void pruneBySize() throws IOException {
        long maxSize = options.maxSize;
        if (maxSize < 0) {
            ServerUtilities.LOGGER.warn("Skipping legacy backup pruning: negative size limit");
            return;
        }
        int keepCount = Math.max(1, options.keepCount);

        List<BackupRetention.Archive> archives = readLegacyBackups();
        Set<File> newest = newestPerWorld(archives);
        long currentSize = archives.stream().mapToLong(archive -> archive.file.length()).sum();
        int remainingCount = archives.size();

        for (BackupRetention.Archive archive : archives) {
            BackupRetention.checkInterrupted();
            if (maxSize > 0 ? currentSize <= maxSize : remainingCount <= keepCount) break;
            if (newest.contains(archive.file)) continue;
            long freed = delete(archive.file, "legacy rotation");
            if (freed >= 0) {
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

    /** Deletes the file and returns the bytes freed, or -1 if it could not be deleted. */
    private static long delete(File file, String reason) {
        long size = file.length();
        try {
            Files.delete(file.toPath());
            ServerUtilities.LOGGER.info("Deleted old backup: {} ({})", file, reason);
            return size;
        } catch (IOException ex) {
            ServerUtilities.LOGGER.warn("Could not delete old backup {}", file, ex);
            return -1;
        }
    }

    /** The most recent archive of each world, which legacy rotation never deletes. Expects oldest-first order. */
    private static Set<File> newestPerWorld(List<BackupRetention.Archive> archives) {
        Set<String> worlds = new HashSet<>();
        Set<File> newest = new HashSet<>();
        for (int i = archives.size() - 1; i >= 0; i--) {
            BackupRetention.Archive archive = archives.get(i);
            if (worlds.add(archive.world)) newest.add(archive.file);
        }
        return newest;
    }

    private List<BackupRetention.Archive> readLegacyBackups() throws IOException {
        List<BackupRetention.Archive> archives = list();
        archives.removeIf(archive -> {
            if (archive.created > System.currentTimeMillis()) {
                ServerUtilities.LOGGER.warn("Preserving future-dated backup {}", archive.file);
                return true;
            }

            if (archive.problem != null) {
                ServerUtilities.LOGGER.warn("Preserving backup with problem {}: {}", archive.file, archive.problem);
                return true;
            }

            return archive.custom && !options.deleteCustom;
        });

        archives.sort(Comparator.comparingLong((BackupRetention.Archive archive) -> archive.file.lastModified())
            .thenComparing(archive -> archive.file.getName()));

        BackupRetention.checkInterrupted();
        return archives;
    }

    private BackupRetention.Plan getRetentionPlan() throws IOException {
        return BackupRetention.plan(options.backupFolder, options.policy, options.deleteCustom, options.maxSize);
    }

    public static class Options {
        public File backupFolder;
        public List<String> policy;
        public boolean deleteCustom;
        public long maxSize;
        public int keepCount;

        public Options(File backupFolder, List<String> policy, boolean deleteCustom, long maxSize, int keepCount) {
            this.backupFolder = backupFolder;
            this.policy = policy;
            this.deleteCustom = deleteCustom;
            this.maxSize = maxSize * FileUtils.SizeUnit.GB.getSize();
            this.keepCount = keepCount;
        }

        public static Options fromConfig() {
            return new Options(
                BackupTask.BACKUP_FOLDER,
                Collections.unmodifiableList(Arrays.asList(backups.retention_policy)),
                backups.delete_custom_name_backups,
                backups.max_folder_size,
                backups.backups_to_keep
            );
        }

        public boolean hasPolicy() {
            return policy != null && !policy.isEmpty();
        }
    }
}