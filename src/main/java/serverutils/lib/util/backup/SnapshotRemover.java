package serverutils.lib.util.backup;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import serverutils.ServerUtilities;
import serverutils.lib.util.FileUtils;

public class SnapshotRemover {

    private static final float DEFAULT_REWRITE_FRACTION = 0.3f;

    private final SnapshotStore store;
    // a pack is rewritten once at least this fraction of its entries are dead
    private final float rewriteFraction;

    public SnapshotRemover(SnapshotStore store, float rewriteFraction) {
        this.store = store;
        this.rewriteFraction = rewriteFraction;
    }

    public SnapshotRemover(SnapshotStore store) {
        this(store, DEFAULT_REWRITE_FRACTION);
    }

    public long remove(Function<List<Snapshot>, List<Snapshot>> picker) throws IOException {
        List<Snapshot> all = store.listAll();
        List<Snapshot> toDelete = picker.apply(all);
        if (toDelete.isEmpty()) return 0;

        Set<String> deleted = new HashSet<>();
        for (Snapshot snapshot : toDelete) {
            deleted.add(snapshot.getName());
        }

        // Everything is computed before anything is deleted: if a kept snapshot has an unreadable manifest this
        // throws, and we stop with all the data still in place.
        Set<SHAHash> liveRegions = new HashSet<>();
        Set<SHAHash> liveChunks = new HashSet<>();
        for (Snapshot snapshot : all) {
            if (deleted.contains(snapshot.getName())) continue;

            for (SnapshotDimension dimension : snapshot.getDimensions().values()) {
                for (SnapshotRegion region : dimension.getRegions()) {
                    liveRegions.add(region.getHash());
                    for (ChunkBlob.Metadata chunk : region.getChunks()) {
                        liveChunks.add(chunk.hash());
                    }
                }
            }
        }

        // delete snapshots first which just leaves unreferenced data (harmless)
        for (Snapshot snapshot : toDelete) {
            store.removeEntry(snapshot);
        }

        // the sweep deletes manifest packs, which can't happen while we hold them open
        store.manifests().closeHandles();

        long freed = sweep(store.packsDirectory, liveChunks);
        freed += sweep(store.manifestsDirectory, liveRegions);

        store.reloadManifests();
        ServerUtilities.LOGGER
                .info("Removed {} snapshots, freed {} bytes", toDelete.size(), FileUtils.getSizeString(freed));
        return freed;
    }

    /**
     * Deletes packs that hold no live entries and rewrites packs where enough of the entries are dead.
     */
    private long sweep(File directory, Set<SHAHash> live) throws IOException {
        long freed = 0;
        List<PackFile> rewrite = new ArrayList<>();

        for (Map.Entry<File, List<PackIndex.Entry>> index : PackIndex.loadAllEntries(directory).entrySet()) {
            PackFile pack = new PackFile(index.getKey(), index.getValue(), live);

            if (pack.isDead()) {
                freed += pack.deadBytes;
                delete(pack);
            } else if (pack.deadFraction() >= rewriteFraction) {
                rewrite.add(pack);
            }
        }

        return freed + rewrite(directory, rewrite);
    }

    private long rewrite(File directory, List<PackFile> packs) throws IOException {
        if (packs.isEmpty()) return 0;

        long freed = 0;
        // an empty known set so every live entry is written, the writer still dedupes across the packs we merge
        try (PackWriter writer = new PackWriter(directory, new HashSet<>())) {
            for (PackFile pack : packs) {
                try (RandomAccessFile in = new RandomAccessFile(pack.packFile(), "r")) {
                    for (PackIndex.Entry entry : pack.entries) {
                        if (!pack.isLive(entry)) continue;

                        byte[] data = new byte[entry.length()];
                        in.seek(entry.offset());
                        in.readFully(data);
                        writer.add(entry.hash(), entry.compressionType(), ByteBuffer.wrap(data));
                    }
                }
                freed += pack.deadBytes;
            }
        }

        for (PackFile pack : packs) {
            delete(pack);
        }
        return freed;
    }

    /** Deletes the index first, a pack without an index is ignored by every loader. */
    private static void delete(PackFile pack) {
        File pack0 = pack.packFile();
        if (!pack.index.delete()) {
            ServerUtilities.LOGGER.error("Failed to delete pack index {}", pack.index);
            return;
        }
        if (pack0.exists() && !pack0.delete()) {
            ServerUtilities.LOGGER.error("Failed to delete pack {}", pack0);
        }
    }

    private static final class PackFile {

        final File index;
        final List<PackIndex.Entry> entries;
        final Set<SHAHash> live;
        final long deadBytes;
        final int deadCount;

        PackFile(File index, List<PackIndex.Entry> entries, Set<SHAHash> live) {
            this.index = index;
            this.entries = entries;
            this.live = live;

            long bytes = 0;
            int count = 0;
            for (PackIndex.Entry entry : entries) {
                if (!live.contains(entry.hash())) {
                    bytes += entry.length();
                    count++;
                }
            }
            this.deadBytes = bytes;
            this.deadCount = count;
        }

        File packFile() {
            String name = index.getName();
            return new File(
                    index.getParentFile(),
                    name.substring(0, name.length() - PackIndex.EXTENSION.length()) + ".pack");
        }

        boolean isLive(PackIndex.Entry entry) {
            return live.contains(entry.hash());
        }

        boolean isDead() {
            return deadCount == entries.size();
        }

        float deadFraction() {
            return (float) deadCount / entries.size();
        }
    }
}
