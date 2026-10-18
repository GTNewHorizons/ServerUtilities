package serverutils.task.backup;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import serverutils.ServerUtilities;
import serverutils.task.backup.repository.ArchiveInfo;
import serverutils.task.backup.repository.Repository;

/**
 * Applies the retention settings to the incremental repository. Archives are chosen like ZIP backups, but the size
 * allowance counts deduplicated data: deleting an archive only frees the chunks no remaining archive uses.
 */
final class RepositoryRetention {

    /** Waste above which compaction rewrites a pack, like borg compact's default threshold. */
    private static final double COMPACT_THRESHOLD = 0.1;

    private RepositoryRetention() {}

    static List<BackupRetention.Archive> readArchives(Repository repository, @Nullable Map<File, ArchiveInfo> infos)
            throws IOException {
        List<BackupRetention.Archive> archives = new ArrayList<>();
        for (String name : repository.archiveNames()) {
            BackupRetention.checkInterrupted();
            File file = repository.archiveFile(name);
            try {
                ArchiveInfo info = repository.readInfo(name);
                String world = info.worldId() != null ? info.worldId()
                        : "name:" + (info.worldName() == null ? "" : info.worldName());
                archives.add(
                        new BackupRetention.Archive(
                                file,
                                world,
                                info.created(),
                                info.originalSize(),
                                info.isCustomName(),
                                null,
                                true));
                if (infos != null) infos.put(file, info);
            } catch (IOException | RuntimeException e) {
                if (e instanceof InterruptedIOException) throw e;
                archives.add(
                        new BackupRetention.Archive(
                                file,
                                null,
                                file.lastModified(),
                                file.length(),
                                false,
                                "Unrecognized/unreadable archive: " + e.getMessage(),
                                true));
            }
        }
        return archives;
    }

    /** With an empty policy, keeps {@code keepCount} archives or applies the size allowance, like ZIP retention. */
    static BackupRetention.Plan plan(Repository repository, String[] policy, int keepCount, long now,
            boolean deleteCustom, long maxSize) throws IOException {
        if (maxSize < 0) throw new IllegalArgumentException("Negative size limit");
        Map<File, ArchiveInfo> infos = new HashMap<>();
        List<BackupRetention.Archive> archives = readArchives(repository, infos);
        BackupRetention.Plan plan;
        List<File> candidates;
        if (policy.length > 0) {
            // Selection only; the size allowance is applied below on deduplicated sizes.
            plan = BackupRetention.select(archives, BackupRetention.parse(policy), now, deleteCustom, 0);
            candidates = new ArrayList<>(plan.sizeCandidates);
        } else {
            candidates = new ArrayList<>();
            plan = selectByCount(archives, keepCount, now, deleteCustom, maxSize > 0, candidates);
        }
        applySizeAllowance(repository, plan, infos, candidates, maxSize);
        return plan;
    }

    private static BackupRetention.Plan selectByCount(List<BackupRetention.Archive> input, int keepCount, long now,
            boolean deleteCustom, boolean bySize, List<File> candidates) {
        List<BackupRetention.Archive> archives = new ArrayList<>(input);
        archives.sort(
                Comparator.comparingLong((BackupRetention.Archive archive) -> archive.created)
                        .thenComparing(archive -> archive.file.getName()));
        BackupRetention.Plan plan = new BackupRetention.Plan();
        List<BackupRetention.Archive> eligible = new ArrayList<>();
        Set<String> worlds = new HashSet<>();
        Set<File> latest = new HashSet<>();
        for (int i = archives.size() - 1; i >= 0; i--) {
            BackupRetention.Archive archive = archives.get(i);
            if (archive.problem != null || archive.created > now || (!deleteCustom && archive.custom)) {
                String reason = archive.problem != null ? archive.problem
                        : archive.created > now ? "Timestamp is in the future" : "Protected custom backup";
                plan.keep.put(archive.file, reason);
                plan.labels.put(
                        archive.file,
                        BackupRetention.reason(
                                archive.problem != null ? "unreadable" : archive.created > now ? "future" : "custom"));
                plan.preserved.add(archive.file);
                plan.sizeExempt.add(archive.file);
                continue;
            }
            eligible.add(0, archive);
            if (worlds.add(archive.world)) latest.add(archive.file);
        }
        int remaining = eligible.size();
        for (BackupRetention.Archive archive : eligible) {
            File file = archive.file;
            if (latest.contains(file)) {
                plan.keep.put(file, "Latest backup for world");
                plan.labels.put(file, BackupRetention.reason("latest"));
            } else if (bySize) {
                candidates.add(file);
                plan.keep.put(file, "Within the size allowance");
                plan.labels.put(file, BackupRetention.reason("rule", "max_folder_size"));
            } else if (remaining > Math.max(1, keepCount)) {
                plan.delete.put(file, "Beyond backups_to_keep");
                plan.labels.put(file, BackupRetention.reason("older"));
                remaining--;
            } else {
                plan.keep.put(file, "Within backups_to_keep");
                plan.labels.put(file, BackupRetention.reason("rule", "backups_to_keep"));
            }
        }
        plan.archives.addAll(archives);
        return plan;
    }

    private static void applySizeAllowance(Repository repository, BackupRetention.Plan plan,
            Map<File, ArchiveInfo> infos, List<File> candidates, long maxSize) throws IOException {
        Repository.Usage usage = repository.usage();
        Repository.Usage exempt = repository.usage();
        long unreadable = 0;
        for (BackupRetention.Archive archive : plan.archives) {
            ArchiveInfo info = infos.get(archive.file);
            if (info == null) {
                unreadable += archive.size;
                continue;
            }
            usage.add(info);
            if (plan.keep.containsKey(archive.file) && plan.sizeExempt.contains(archive.file)) exempt.add(info);
        }
        long before = usage.liveBytes();
        for (File file : plan.delete.keySet()) {
            ArchiveInfo info = infos.get(file);
            if (info != null) usage.remove(info);
        }
        // Protected archives do not count toward the allowance, only what the rotating ones add on top of them.
        long base = exempt.liveBytes();
        for (File file : candidates) {
            if (maxSize <= 0 || usage.liveBytes() - base <= maxSize) break;
            ArchiveInfo info = infos.get(file);
            if (info == null || !plan.keep.containsKey(file)) continue;
            usage.remove(info);
            plan.keep.remove(file);
            plan.keptUntil.remove(file);
            plan.delete.put(file, "Folder size limit");
            plan.labels.put(file, BackupRetention.reason("size"));
        }
        plan.remainingSize = usage.liveBytes() + unreadable;
        plan.remainingRotationSize = usage.liveBytes() - base;
        plan.deletedSize = before - usage.liveBytes();
    }

    /** Deletes the planned archives, then the chunks only they used. */
    static void apply(Repository repository, BackupRetention.Plan plan, long maxSize) throws IOException {
        int deleted = 0;
        for (Map.Entry<File, String> decision : plan.delete.entrySet()) {
            BackupRetention.checkInterrupted();
            File file = decision.getKey();
            try {
                Files.delete(file.toPath());
                deleted++;
                ServerUtilities.LOGGER.info("Deleted old incremental backup: {} ({})", file, decision.getValue());
            } catch (IOException ex) {
                ServerUtilities.LOGGER.warn("Could not delete old incremental backup {}", file, ex);
            }
        }
        for (Map.Entry<File, String> decision : plan.keep.entrySet()) {
            if (decision.getValue().startsWith("Unrecognized/unreadable")) {
                ServerUtilities.LOGGER
                        .warn("Preserving incremental backup {}: {}", decision.getKey(), decision.getValue());
            }
        }
        if (maxSize > 0 && plan.remainingRotationSize > maxSize) {
            ServerUtilities.LOGGER.warn(
                    "Incremental backup size allowance could not be met: {} counted bytes remain; protected backups or failed deletions",
                    plan.remainingRotationSize);
        }
        if (deleted == 0) return;
        Repository.CompactResult result = repository.compact(COMPACT_THRESHOLD);
        for (String warning : repository.warnings()) ServerUtilities.LOGGER.warn("Backup repository: {}", warning);
        ServerUtilities.LOGGER.info(
                "Compacted backup repository: freed {} bytes, deleted {} and rewrote {} packs",
                result.freedBytes,
                result.deletedPacks,
                result.rewrittenPacks);
    }
}
