package serverutils.lib.util.backup;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.Nullable;

import cpw.mods.fml.common.Loader;
import lombok.Getter;
import serverutils.ServerUtilities;
import serverutils.lib.util.StringUtils;
import serverutils.task.backup.BackupCandidates;
import serverutils.task.backup.WorldFile;

public class SnapshotWriter {

    // files are read into memory whole
    private static final long MAX_FILE_SIZE = 512L * 1024 * 1024;
    private static final int READ_ATTEMPTS = 3;
    private static final String SESSION_LOCK = "session.lock";

    private final SnapshotStore store;
    private final ChunkReader reader;

    @Getter
    private SnapshotStats stats = new SnapshotStats();

    public SnapshotWriter(SnapshotStore store, ChunkReader reader) {
        this.store = store;
        this.reader = reader;
    }

    public Snapshot write(BackupCandidates candidates, @Nullable String label) throws IOException {
        long started = System.nanoTime();
        stats = new SnapshotStats();
        ZonedDateTime createdAt = ZonedDateTime.now(ZoneOffset.UTC);

        File packs = store.packsDirectory;
        File filePacks = store.filesDirectory;
        File manifestPacks = store.manifestsDirectory;

        Set<SHAHash> known;
        Set<SHAHash> knownFiles;
        Set<SHAHash> knownManifests;
        try {
            known = PackIndex.loadKnownHashes(packs);
            knownFiles = PackIndex.loadKnownHashes(filePacks);
            knownManifests = PackIndex.loadKnownHashes(manifestPacks);
        } catch (IOException e) {
            throw new IOException("Failed to read existing pack indexes", e);
        }

        Snapshot previous = store.findLatest();

        Map<String, List<WorldFile>> filesByDimension = candidates.getFilesByDimension();
        Map<String, SnapshotDimension> dimensions = new HashMap<>();
        Progress progress = new Progress(filesByDimension.values().stream().mapToInt(List::size).sum());
        try (PackWriter packWriter = new PackWriter(packs, known);
                PackWriter fileWriter = new PackWriter(filePacks, knownFiles);
                PackWriter manifestWriter = new PackWriter(manifestPacks, knownManifests)) {

            for (String dimensionId : candidates.dimensions()) {
                List<WorldFile> files = filesByDimension.get(dimensionId);
                SnapshotDimension previousDim = previous == null ? null : previous.getDimension(dimensionId);
                SnapshotDimension newDim = writeDimension(
                        dimensionId,
                        files,
                        previousDim,
                        progress,
                        packWriter,
                        fileWriter,
                        manifestWriter);
                dimensions.put(dimensionId, newDim);
            }

            stats.newChunks = packWriter.getBlobsWritten();
            stats.newFiles = fileWriter.getBlobsWritten();
            stats.newBytes = packWriter.getBytesWritten() + fileWriter.getBytesWritten();
            stats.dedupedChunks = packWriter.getBlobsDeduplicated();
            stats.dedupedBytes = packWriter.getBytesDeduplicated();
        } catch (IOException e) {
            throw new IOException("Failed to write snapshot data", e);
        }

        Snapshot snapshot = new Snapshot(
                store.worldName(),
                label,
                createdAt,
                ZonedDateTime.now(ZoneOffset.UTC),
                Loader.instance().getMCVersionString(),
                ServerUtilities.VERSION, // this version is kinda a pain, maybe replace it with a snapshot-specific code
                dimensions);

        store.write(snapshot);

        stats.durationMillis = (System.nanoTime() - started) / 1_000_000L;
        ServerUtilities.LOGGER.info(
                "Snapshot {} completed in {} ms: {} new chunks and {} new files ({} bytes), {} deduplicated chunks ({} bytes), {} unmodified chunks, {} unmodified regions, {} unmodified files",
                snapshot.getLabel(),
                stats.durationMillis,
                stats.newChunks,
                stats.newFiles,
                stats.newBytes,
                stats.dedupedChunks,
                stats.dedupedBytes,
                stats.unmodifiedChunks,
                stats.unmodifiedRegions,
                stats.unmodifiedMiscFiles);
        return snapshot;
    }

    private SnapshotDimension writeDimension(String id, List<WorldFile> files, SnapshotDimension previous,
            Progress progress, PackWriter packWriter, PackWriter fileWriter, PackWriter manifestWriter)
            throws IOException {
        SnapshotDimension dimension = new SnapshotDimension(id);

        for (WorldFile file : files) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Snapshot was interrupted");
            progress.next(file);
            if (file.name().equals(SESSION_LOCK)) continue;

            SnapshotEntry previousEntry = previous == null ? null : previous.getEntry(file);

            if (file.isUnmodified(previousEntry)) {
                dimension.addEntry(previousEntry);
                stats.unmodified(previousEntry);
                continue;
            }

            File backing = file.backing();
            switch (file.kind()) {
                case REGION -> dimension.addRegion(
                        writeRegion(
                                backing,
                                SnapshotRegion.nameOf(backing),
                                (SnapshotRegion) previousEntry,
                                packWriter,
                                manifestWriter));
                case MISC -> {
                    if (file.size() > MAX_FILE_SIZE) {
                        ServerUtilities.LOGGER.warn(
                                "File {} is too large to be saved in a snapshot. This file will be skipped in this version of ServerUtilities.",
                                file);
                        continue;
                    }
                    dimension.addFile(writeFile(file, fileWriter));
                }
            }
        }

        return dimension;
    }

    private SnapshotFile writeFile(WorldFile source, PackWriter fileWriter) throws IOException {
        File file = source.backing();

        if (file.length() > MAX_FILE_SIZE) {
            throw new IOException("File " + file + " is too large to be saved in a snapshot");
        }

        long mtime;
        byte[] data;
        int attempt = 0;
        while (true) {
            mtime = file.lastModified();
            try {
                data = Files.readAllBytes(file.toPath());
            } catch (IOException e) {
                throw new IOException("Failed to read file " + file, e);
            }
            if (file.lastModified() == mtime && file.length() == data.length) break;
            if (++attempt == READ_ATTEMPTS) {
                // the mtime we keep is from before the read, so the next snapshot sees the file as changed
                ServerUtilities.LOGGER.warn("File {} kept changing while it was being saved", file);
                break;
            }
        }

        byte[] stored = BlobCompression.deflate(data);
        int compression = BlobCompression.ZLIB;
        if (stored.length >= data.length) {
            stored = data;
            compression = BlobCompression.NONE;
        }

        SHAHash hash = SHAHash.compute(compression, stored);
        fileWriter.add(hash, compression, ByteBuffer.wrap(stored));
        return new SnapshotFile(source.relativePath(), hash, data.length, mtime);
    }

    private SnapshotRegion writeRegion(File regionFile, String name, SnapshotRegion previousRegion,
            PackWriter packWriter, PackWriter manifestWriter) throws IOException {
        long mtime = regionFile.lastModified();
        long size = regionFile.length();

        List<ChunkBlob> blobs;
        try {
            blobs = reader.readChunks(new RegionChunkFile(regionFile.toPath()));
        } catch (IOException e) {
            // by no means should we recover!! since snapshots are incremental, falsely saving a chunk like this poisons
            // the saved data!!! (DO NOT RECOVER!)
            throw new IOException("Failed to read chunks from region file " + regionFile, e);
        }

        if (previousRegion != null) {
            try {
                previousRegion.getChunks(); // force the manifest to load now
            } catch (IOException e) {
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
                // only drop the data when it is really stored, otherwise the chunk is written again below
                if (previousMeta.hash().equals(meta.hash()) && packWriter.contains(meta.hash())) {
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

    /** Logs progress at most every 5 seconds, and always for the last file. */
    private static final class Progress {

        private final int total;
        private int index;
        private long logTimer = System.currentTimeMillis();

        private Progress(int total) {
            this.total = total;
        }

        private void next(WorldFile file) {
            if (index == total - 1 || System.currentTimeMillis() >= logTimer) {
                logTimer = System.currentTimeMillis() + 5000;
                ServerUtilities.LOGGER.info(
                        "[{} | {}%]: {}",
                        index,
                        StringUtils.formatDouble00((index / (double) total) * 100D),
                        file.backing().getAbsolutePath());
            }
            index++;
        }
    }
}
