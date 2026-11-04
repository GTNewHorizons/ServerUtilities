package serverutils.task.backup;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import serverutils.ServerUtilities;
import serverutils.task.backup.repository.ArchiveInfo;
import serverutils.task.backup.repository.Repository;

/**
 * The archives of the incremental repository. They share chunks, so sizes are deduplicated: deleting an archive only
 * frees the chunks no remaining archive uses, and those are only freed on disk once {@link #reclaim} compacts the
 * packs. The repository stays open, and locked, until {@link #close}.
 */
public final class RepositoryStore implements BackupStore<RepositoryStore.Entry>, Closeable {

    /** Commands and pruning give up quickly if the repository tool is holding the repository. */
    private static final long LOCK_TIMEOUT_MILLIS = 5_000;
    /** Waste above which compaction rewrites a pack, like borg compact's default threshold. */
    private static final double COMPACT_THRESHOLD = 0.1;

    private final File root;
    private Repository repository;
    private List<Entry> entries = Collections.emptyList();
    /** Kept between calls, since retention asks for the sizes of sets that differ by one archive at a time. */
    private final Usage union = new Usage();
    private final Usage base = new Usage();
    private boolean deleted;

    public RepositoryStore(File root) {
        this.root = root;
    }

    @Override
    public List<Entry> list() throws IOException {
        Repository repository = open();
        List<Entry> entries = new ArrayList<>();
        for (String name : repository.archiveNames()) {
            BackupRetention.checkInterrupted();
            File file = repository.archiveFile(name);
            try {
                entries.add(new Entry(name, file, repository.readInfo(name), null));
            } catch (InterruptedIOException e) {
                throw e;
            } catch (IOException | RuntimeException e) {
                entries.add(new Entry(name, file, null, "Unrecognized/unreadable archive: " + e.getMessage()));
            }
        }
        this.entries = entries;
        return entries;
    }

    @Override
    public void delete(Collection<Entry> entries, BackupResult.Retention result) throws IOException {
        Repository repository = open();
        List<Entry> removed = new ArrayList<>();
        for (Entry entry : entries) {
            try {
                repository.deleteArchive(entry.name);
                removed.add(entry);
                ServerUtilities.LOGGER.info("Deleted old incremental backup: {}", entry.file);
            } catch (IOException ex) {
                ServerUtilities.LOGGER.warn("Could not delete old incremental backup {}", entry.file, ex);
            }
        }
        if (removed.isEmpty()) return;
        List<Entry> remaining = new ArrayList<>(this.entries);
        remaining.removeAll(removed);
        result.bytesRemoved += countedSize(removed, remaining);
        result.filesRemoved += removed.size();
        this.entries = remaining;
        deleted = true;
    }

    @Override
    public long countedSize(Collection<Entry> entries, Collection<Entry> base) {
        try {
            Set<Entry> all = new HashSet<>(base);
            all.addAll(entries);
            return union.of(all) - this.base.of(base);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void reclaim() throws IOException {
        if (!deleted) return;
        deleted = false;
        // Compaction moves chunks, so usage counted before it no longer applies.
        union.clear();
        base.clear();
        Repository.CompactResult result = open().compact(COMPACT_THRESHOLD);
        logWarnings();
        ServerUtilities.LOGGER.info(
                "Compacted backup repository: freed {} bytes, deleted {} and rewrote {} packs",
                result.freedBytes,
                result.deletedPacks,
                result.rewrittenPacks);
    }

    @Override
    public void close() throws IOException {
        union.clear();
        base.clear();
        entries = Collections.emptyList();
        if (repository == null) return;
        try {
            repository.close();
        } finally {
            repository = null;
        }
    }

    private Repository open() throws IOException {
        if (repository == null) {
            repository = Repository.open(root, true, LOCK_TIMEOUT_MILLIS);
            logWarnings();
        }
        return repository;
    }

    private void logWarnings() {
        for (String warning : repository.warnings()) ServerUtilities.LOGGER.warn("Backup repository: {}", warning);
    }

    /** The deduplicated size of a set of archives, updated by the difference from the last set it was asked for. */
    private final class Usage {

        private final Set<Entry> members = new HashSet<>();
        private Repository.Usage usage;
        /** Unreadable archives cannot be deduplicated, so they count their file size. */
        private long unreadable;

        long of(Collection<Entry> target) throws IOException {
            if (usage == null) usage = open().usage();
            for (Iterator<Entry> it = members.iterator(); it.hasNext();) {
                Entry entry = it.next();
                if (target.contains(entry)) continue;
                update(entry, false);
                it.remove();
            }
            for (Entry entry : target) {
                if (members.add(entry)) update(entry, true);
            }
            return usage.liveBytes() + unreadable;
        }

        private void update(Entry entry, boolean add) throws IOException {
            BackupRetention.checkInterrupted();
            if (entry.info == null) unreadable += add ? entry.size : -entry.size;
            else if (add) usage.add(entry.info);
            else usage.remove(entry.info);
        }

        void clear() {
            members.clear();
            usage = null;
            unreadable = 0;
        }
    }

    public static final class Entry implements BackupEntry {

        final String name;
        public final File file;
        @Nullable
        final ArchiveInfo info;
        @Nullable
        private final String problem;
        private final long size;

        Entry(String name, File file, @Nullable ArchiveInfo info, @Nullable String problem) {
            this.name = name;
            this.file = file;
            this.info = info;
            this.problem = problem;
            this.size = info != null ? info.originalSize() : file.length();
        }

        @Override
        public String id() {
            return file.getName();
        }

        @Override
        @Nullable
        public String world() {
            if (info == null) return null;
            if (info.worldId() != null) return info.worldId();
            return "name:" + (info.worldName() == null ? "" : info.worldName());
        }

        @Override
        public long createdAt() {
            return info != null ? info.created() : file.lastModified();
        }

        /** The original size of the archive's files; what it takes up in the repository depends on the others. */
        @Override
        public long size() {
            return size;
        }

        @Override
        public boolean isCustom() {
            return info != null && info.isCustomName();
        }

        @Override
        @Nullable
        public String problem() {
            return problem;
        }

        /** Names are unique within a repository, however its folder was spelled. */
        @Override
        public boolean equals(Object other) {
            return other instanceof Entry entry && entry.name.equals(name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return BackupEntry.toString(this);
        }
    }
}
