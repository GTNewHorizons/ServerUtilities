package serverutils.lib.util.backup;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import serverutils.lib.util.FileUtils;

/**
 * Writes blobs into packs, starting a new pack whenever the current one would exceed maxPacketSize. A single blob
 * larger than the limit still gets a complete pack of its own.
 */
public class PackWriter implements Closeable {

    public static final long DEFAULT_MAX_PACK_SIZE = FileUtils.SizeUnit.MB.getSize() * 512;

    private final File directory;
    private final Set<SHAHash> known;
    private final long maxPackSize;
    private final String namePrefix = "pack-" + System.currentTimeMillis();
    private final List<PackIndex.Entry> entries = new ArrayList<>();

    @Getter
    private int packCount;
    private String name;
    private File tempPack;
    private FileChannel channel;
    private long position; // bytes written to the current pack
    @Getter
    private int blobsWritten;
    @Getter
    private long bytesWritten;
    @Getter
    private int blobsDeduplicated;
    @Getter
    private long bytesDeduplicated;

    public PackWriter(File directory, Set<SHAHash> known) {
        this(directory, known, DEFAULT_MAX_PACK_SIZE);
    }

    public PackWriter(File directory, Set<SHAHash> known, long maxPackSize) {
        this.directory = directory;
        this.known = known;
        this.maxPackSize = maxPackSize;
    }

    public boolean contains(SHAHash hash) {
        return known.contains(hash);
    }

    public boolean add(ChunkBlob blob) throws IOException {
        if (known.contains(blob.hash())) {
            int skipped = blob.data().remaining(); // 0 for references to earlier snapshots
            if (skipped > 0) {
                blobsDeduplicated++;
                bytesDeduplicated += skipped;
            }
            return false;
        }

        // a reference to an earlier snapshot whose data is gone, storing it would record the hash with no data
        if (!blob.data().hasRemaining()) {
            throw new IOException("Chunk " + blob.hash() + " is referenced but is not stored in any pack");
        }

        known.add(blob.hash());
        write(blob.hash(), blob.metadata().compressionType(), blob.data());
        return true;
    }

    public boolean add(SnapshotRegion region) throws IOException {
        return add(region.hash(), 0, ByteBuffer.wrap(region.toBytes()));
    }

    public boolean add(SHAHash hash, int compression, ByteBuffer data) throws IOException {
        if (!known.add(hash)) return false;
        write(hash, compression, data);
        return true;
    }

    @Override
    public void close() throws IOException {
        finishPack();
    }

    /**
     * Seals the current pack, if any. The pack is moved into place before its index is written, so an index never
     * exists without its pack (a pack without an index is ignored by every loader).
     */
    private void finishPack() throws IOException {
        if (channel == null) return; // nothing new to store

        channel.force(true);
        channel.close();
        channel = null;

        File pack = new File(directory, name + ".pack");
        Files.move(tempPack.toPath(), pack.toPath(), StandardCopyOption.REPLACE_EXISTING);
        PackIndex.write(new File(directory, name + PackIndex.EXTENSION), entries);

        entries.clear();
        position = 0;
    }

    private void write(SHAHash hash, int compression, ByteBuffer data) throws IOException {
        int length = data.remaining();
        if (channel != null && position > 0 && position + length > maxPackSize) finishPack();

        FileChannel channel = getChannel();
        while (data.hasRemaining()) {
            channel.write(data);
        }

        entries.add(new PackIndex.Entry(hash, position, length, compression));
        position += length;
        bytesWritten += length;
        blobsWritten++;
    }

    private FileChannel getChannel() throws IOException {
        if (channel != null) return channel;
        name = namePrefix + "-" + packCount++;
        tempPack = new File(directory, name + ".pack.tmp");
        channel = FileChannel.open(
                tempPack.toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
        return channel;
    }
}
