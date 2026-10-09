package serverutils.lib.util.backup;

import java.io.File;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.github.bsideup.jabel.Desugar;

import cpw.mods.fml.common.Loader;
import serverutils.ServerUtilities;

public class SnapshotWriter {

    private final SnapshotStore store;
    private final ChunkReader reader;
    private SnapshotStats stats = new SnapshotStats();

    public SnapshotWriter(SnapshotStore store, ChunkReader reader) {
        this.store = store;
        this.reader = reader;
    }

    public SnapshotStats getStats() {
        return stats;
    }

    public Snapshot write(File source) {
        if (!source.isDirectory()) throw new IllegalArgumentException("Not a directory");

        long started = System.nanoTime();
        stats = new SnapshotStats();
        ZonedDateTime createdAt = ZonedDateTime.now(ZoneOffset.UTC);

        List<SourceDimension> sources = discoverDimensions(source);

        File packs = store.packsDirectory;
        File manifestPacks = store.manifestsDirectory;

        Set<SHAHash> known;
        Set<SHAHash> knownManifests;
        try {
            known = PackIndex.loadKnownHashes(packs);
            knownManifests = PackIndex.loadKnownHashes(manifestPacks);
        } catch (IOException e) {
            throw new SnapshotException("Failed to read existing pack indexes", e);
        }

        Snapshot previous = store.findLatest();

        Map<Integer, SnapshotDimension> dimensions = new HashMap<>();
        try (PackWriter packWriter = new PackWriter(packs, known);
                PackWriter manifestWriter = new PackWriter(manifestPacks, knownManifests)) {
            for (SourceDimension sourceDimension : sources) {
                SnapshotDimension previousDimension = previous == null ? null
                        : previous.getDimension(sourceDimension.id);
                dimensions.put(
                        sourceDimension.id,
                        writeDimension(sourceDimension, previousDimension, packWriter, manifestWriter));
            }
            stats.newChunks = packWriter.getBlobsWritten();
            stats.newBytes = packWriter.getBytesWritten();
            stats.dedupedChunks = packWriter.getBlobsDeduplicated();
            stats.dedupedBytes = packWriter.getBytesDeduplicated();
        } catch (IOException e) {
            throw new SnapshotException("Failed to write snapshot data", e);
        }

        Snapshot snapshot = new Snapshot(
                createdAt,
                ZonedDateTime.now(ZoneOffset.UTC),
                Loader.instance().getMCVersionString(),
                ServerUtilities.VERSION, // this version is kinda a pain, maybe replace it with a snapshot-specific code
                                         // version
                new HashMap<>(),
                dimensions);

        store.write(snapshot);

        stats.durationMillis = (System.nanoTime() - started) / 1_000_000L;
        ServerUtilities.LOGGER.info(
                "Snapshot {} completed in {} ms: {} new chunks ({} bytes), {} deduplicated chunks ({} bytes), {} unmodified chunks, {} unmodified files",
                snapshot.getName(),
                stats.durationMillis,
                stats.newChunks,
                stats.newBytes,
                stats.dedupedChunks,
                stats.dedupedBytes,
                stats.unmodifiedChunks,
                stats.unmodifiedFiles);
        return snapshot;
    }

    private SnapshotDimension writeDimension(SourceDimension source, SnapshotDimension previous, PackWriter packWriter,
            PackWriter manifestWriter) throws IOException {
        SnapshotDimension dimension = new SnapshotDimension(source.id);

        for (File regionFile : source.regionFiles) {
            if (Thread.currentThread().isInterrupted()) throw new SnapshotException("Snapshot was interrupted");

            String name = SnapshotRegion.nameOf(regionFile);
            SnapshotRegion previousRegion = previous == null ? null : previous.getRegion(name);

            if (previousRegion != null && previousRegion.mtime == regionFile.lastModified()
                    && previousRegion.size == regionFile.length()) {
                dimension.addRegion(previousRegion);
                stats.unmodifiedFiles++;
                continue;
            }

            dimension.addRegion(writeRegion(regionFile, name, previousRegion, packWriter, manifestWriter));
        }

        return dimension;
    }

    private SnapshotRegion writeRegion(File regionFile, String name, SnapshotRegion previousRegion,
            PackWriter packWriter, PackWriter manifestWriter) throws IOException {
        long mtime = regionFile.lastModified();
        long size = regionFile.length();

        List<ChunkBlob> blobs;
        try {
            blobs = reader.readChunks(new RegionChunkFile(regionFile.toPath()));
        } catch (Exception e) {
            // by no means should we recover!! since snapshots are incremental, falsely saving a chunk like this poisons
            // the saved data!!! (DO NOT RECOVER!)
            throw new SnapshotException("Failed to read chunks from region file " + regionFile, e);
        }

        if (previousRegion != null) {
            try {
                previousRegion.getChunks(); // force the manifest to load now
            } catch (SnapshotException e) {
                // unlike the above error, this just means that we save the region again so no harm recovering
                ServerUtilities.LOGGER.warn("Ignoring previous snapshot of region {}", name, e);
                previousRegion = null;
            }
        }

        TreeMap<Integer, ChunkBlob.Metadata> chunks = new TreeMap<>();
        for (ChunkBlob blob : blobs) {
            ChunkBlob.Metadata meta = blob.metadata();
            ChunkBlob.Metadata previousMeta = previousRegion == null ? null : previousRegion.getChunk(meta.index());

            if (previousMeta != null) {
                if (previousMeta.timestamp() == meta.timestamp() || previousMeta.hash().equals(meta.hash())) {
                    blob = new ChunkBlob.Empty(previousMeta);
                    stats.unmodifiedChunks++;
                }
            }

            chunks.put(blob.metadata().index(), blob.metadata());
            packWriter.add(blob);
        }

        SnapshotRegion region = SnapshotRegion.create(name, mtime, size, chunks);
        manifestWriter.add(region);
        return region;
    }

    private static List<SourceDimension> discoverDimensions(File worldDirectory) {
        File[] directories = worldDirectory.listFiles(File::isDirectory);
        if (directories == null) throw new SnapshotException(
                "No regions or dimensions were found in world folder, likely the specified world folder is incorrect");

        List<SourceDimension> discovered = new ArrayList<>();
        // TODO: cross check to make sure this is robust enough
        for (File directory : directories) {
            String dirName = directory.getName();
            if (!dirName.startsWith("DIM") && !dirName.equals("region")) continue;

            int id = 0;
            File regionDirectory = directory;
            if (dirName.startsWith("DIM")) {
                id = Integer.parseInt(dirName.substring(3));
                regionDirectory = new File(directory, "region");
            }

            File[] regionFiles = regionDirectory.listFiles(f -> f.getName().endsWith(".mca"));
            discovered.add(
                    new SourceDimension(
                            id,
                            regionFiles != null ? Arrays.asList(regionFiles) : Collections.emptyList()));
        }
        return discovered;
    }

    @Desugar
    private record SourceDimension(int id, List<File> regionFiles) {}
}
