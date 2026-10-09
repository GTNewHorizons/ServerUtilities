package serverutils.lib.util.backup;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
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

    // files are read into memory whole
    private static final long MAX_FILE_SIZE = 512L * 1024 * 1024;
    private static final int READ_ATTEMPTS = 3;
    private static final String SESSION_LOCK = "session.lock";

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
            throw new SnapshotException("Failed to read existing pack indexes", e);
        }

        Snapshot previous = store.findLatest();

        Map<String, SnapshotDimension> dimensions = new HashMap<>();
        try (PackWriter packWriter = new PackWriter(packs, known);
                PackWriter fileWriter = new PackWriter(filePacks, knownFiles);
                PackWriter manifestWriter = new PackWriter(manifestPacks, knownManifests)) {
            for (SourceDimension sourceDimension : sources) {
                SnapshotDimension previousDimension = previous == null ? null
                        : previous.getDimension(sourceDimension.id);
                dimensions.put(
                        sourceDimension.id,
                        writeDimension(sourceDimension, previousDimension, packWriter, fileWriter, manifestWriter));
            }
            stats.newChunks = packWriter.getBlobsWritten();
            stats.newMiscFiles = fileWriter.getBlobsWritten();
            stats.newBytes = packWriter.getBytesWritten() + fileWriter.getBytesWritten();
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
                dimensions);

        store.write(snapshot);

        stats.durationMillis = (System.nanoTime() - started) / 1_000_000L;
        ServerUtilities.LOGGER.info(
                "Snapshot {} completed in {} ms: {} new chunks and {} new files ({} bytes), {} deduplicated chunks ({} bytes), {} unmodified chunks, {} unmodified regions, {} unmodified files",
                snapshot.getName(),
                stats.durationMillis,
                stats.newChunks,
                stats.newMiscFiles,
                stats.newBytes,
                stats.dedupedChunks,
                stats.dedupedBytes,
                stats.unmodifiedChunks,
                stats.unmodifiedFiles,
                stats.unmodifiedMiscFiles);
        return snapshot;
    }

    private SnapshotDimension writeDimension(SourceDimension source, SnapshotDimension previous, PackWriter packWriter,
            PackWriter fileWriter, PackWriter manifestWriter) throws IOException {
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

        for (SourceFile file : source.files) {
            if (Thread.currentThread().isInterrupted()) throw new SnapshotException("Snapshot was interrupted");

            SnapshotFile previousFile = previous == null ? null : previous.getFile(file.path);

            // I don't believe any file should hit this size, if it is hitting this size it (likely) shouldn't be backed up anyway
            // if in the future there is a valid use case here, file can be chunked, but that involves changing up the blob format
            if (file.file.length() > MAX_FILE_SIZE) {
                ServerUtilities.LOGGER.warn("File {} is too large to be saved in a snapshot. This file will be skipped in this version of ServerUtilities.", file);
                continue;
            }

            dimension.addFile(writeFile(file, previousFile, fileWriter));
        }

        return dimension;
    }

    private SnapshotFile writeFile(SourceFile source, SnapshotFile previous, PackWriter fileWriter) throws IOException {
        File file = source.file;
        if (previous != null && previous.mtime() == file.lastModified() && previous.size() == file.length()) {
            stats.unmodifiedMiscFiles++;
            return previous;
        }

        if (file.length() > MAX_FILE_SIZE) {
            throw new SnapshotException("File " + file + " is too large to be saved in a snapshot");
        }

        long mtime;
        byte[] data;
        int attempt = 0;
        while (true) {
            mtime = file.lastModified();
            try {
                data = Files.readAllBytes(file.toPath());
            } catch (IOException e) {
                throw new SnapshotException("Failed to read file " + file, e);
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
        return new SnapshotFile(source.path, hash, data.length, mtime);
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

        // the overworld always exists, its folder is the world folder itself (level.dat is saved even without regions)
        List<SourceDimension> discovered = new ArrayList<>();
        discovered.add(sourceDimension(SnapshotDimension.OVERWORLD, worldDirectory, true));

        for (File directory : directories) {
            if (isDimensionFolder(directory)) discovered.add(sourceDimension(directory.getName(), directory, false));
        }
        return discovered;
    }

    private static boolean isDimensionFolder(File directory) {
        return directory.getName().startsWith("DIM") || new File(directory, "region").isDirectory();
    }

    private static SourceDimension sourceDimension(String id, File directory, boolean isWorldFolder) {
        File[] regionFiles = new File(directory, "region").listFiles(f -> f.getName().endsWith(".mca"));

        List<SourceFile> files = new ArrayList<>();
        collectFiles(directory, directory, files, isWorldFolder);
        return new SourceDimension(
                id,
                regionFiles != null ? Arrays.asList(regionFiles) : Collections.emptyList(),
                files);
    }

    private static void collectFiles(File root, File directory, List<SourceFile> out, boolean isWorldFolder) {
        File[] children = directory.listFiles();
        if (children == null) return;

        boolean isTopLevel = root == directory;

        for (File child : children) {
            String name = child.getName();
            if (child.isDirectory()) {
                if (isTopLevel && (name.equals("region") || (isWorldFolder && isDimensionFolder(child)))) {
                    continue;
                }
                collectFiles(root, child, out, isWorldFolder);
            } else if (!name.equals(SESSION_LOCK) && child.isFile()) {
                String path = root.toPath().relativize(child.toPath()).toString().replace(File.separatorChar, '/');
                out.add(new SourceFile(path, child));
            }
        }
    }

    @Desugar
    private record SourceFile(String path, File file) {}

    @Desugar
    private record SourceDimension(String id, List<File> regionFiles, List<SourceFile> files) {}
}
