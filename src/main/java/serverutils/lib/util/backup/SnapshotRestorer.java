package serverutils.lib.util.backup;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import serverutils.lib.util.FileUtils;

public class SnapshotRestorer {

    private final SnapshotStore store;
    private final ChunkWriter writer;

    public SnapshotRestorer(SnapshotStore store, ChunkWriter writer) {
        this.store = store;
        this.writer = writer;
    }

    public static void restore(SnapshotStore store, Snapshot snapshot, File destination) throws IOException {
        SnapshotRestorer restorer = new SnapshotRestorer(store, new SimpleChunkWriter());
        restorer.restore(snapshot, destination);
    }

    public void restore(Snapshot snapshot, File destination) throws IOException {
        File savesDirectory = new File("saves/");
        File temporary = new File(savesDirectory, destination.getName() + "-" + snapshot.createdAt + ".restored");

        for (SnapshotDimension dimension : snapshot.getDimensions().values()) {
            restore(temporary, dimension);
        }

        moveAtomic(temporary, destination);
    }

    private void restore(File root, SnapshotDimension dimension) throws IOException {
        PackStore fileStore = store.files();

        File dimensionDir = root;
        if (!dimension.isOverworld()) {
            dimensionDir = new File(root, dimension.getDirName());
        }

        FileUtils.ensureExists(dimensionDir);

        for (SnapshotFile file : dimension.getFiles()) {
            PackStore.Blob blob = fileStore.readBlob(file.hash());
            byte[] data = BlobCompression.decompress(blob.compression(), blob.data());

            File destination = new File(dimensionDir, file.path());
            File parent = destination.getParentFile();
            if (parent != null) FileUtils.ensureExists(parent);

            Files.write(destination.toPath(), data);
        }

        File regionDir = new File(dimensionDir, "region");
        FileUtils.ensureExists(regionDir);

        PackStore chunkPacks = store.chunks();

        for (SnapshotRegion region : dimension.getRegions()) {
            File regionFile = new File(regionDir, region.name + ".mca");
            ChunkFile chunkFile = new RegionChunkFile(regionFile.toPath());

            List<ChunkBlob.Metadata> chunks = region.getChunks();
            List<ChunkBlob> blobs = new ArrayList<>(chunks.size());

            for (ChunkBlob.Metadata metadata : region.getChunks()) {
                PackStore.Blob blob = chunkPacks.readBlob(metadata.hash());
                // recompute hash to verify blob actually matches expected hash
                SHAHash blobHash = SHAHash.compute(blob.compression(), blob.data());
                if (!blobHash.equals(metadata.hash())) {
                    throw new SnapshotException(
                            "Chunk blob hash mismatch for region " + region.name + " chunk " + metadata.index());
                }
                blobs.add(new SimpleChunkBlob(metadata, ByteBuffer.wrap(blob.data())));
            }

            writer.write(chunkFile, blobs);
        }
    }

    private static void moveAtomic(File source, File destination) throws IOException {
        Path sourcePath = source.toPath();
        Path destinationPath = destination.toPath();

        Path oldPath = Paths.get(destinationPath + "_old");
        int i = 1;
        while (oldPath.toFile().exists()) oldPath = Paths.get(oldPath.toString() + i++);

        if (!destination.exists()) {
            Files.move(sourcePath, destinationPath, StandardCopyOption.ATOMIC_MOVE);
            return;
        }

        try {
            Files.move(destinationPath, oldPath, StandardCopyOption.ATOMIC_MOVE);
            Files.move(sourcePath, destinationPath, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            if (oldPath.toFile().exists()) Files.move(oldPath, destinationPath, StandardCopyOption.ATOMIC_MOVE);
            throw e;
        }
    }
}
