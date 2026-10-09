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

    private int packCount;
    private String name;
    private File tempPack;
    private FileChannel channel;
    private long position; // bytes written to the current pack
    private int blobsWritten;
    private long bytesWritten;
    private int blobsDeduplicated;
    private long bytesDeduplicated;

    public PackWriter(File directory, Set<SHAHash> known) {
        this(directory, known, DEFAULT_MAX_PACK_SIZE);
    }

    public PackWriter(File directory, Set<SHAHash> known, long maxPackSize) {
        this.directory = directory;
        this.known = known;
        this.maxPackSize = maxPackSize;
    }

    public boolean add(ChunkBlob blob) throws IOException {
        if (!known.add(blob.getHash())) {
            int skipped = blob.data().remaining(); // 0 for references to earlier snapshots
            if (skipped > 0) {
                blobsDeduplicated++;
                bytesDeduplicated += skipped;
            }
            return false;
        }

        write(blob.getHash(), blob.metadata().compressionType(), blob.data());
        return true;
    }

    public boolean add(SnapshotRegion region) throws IOException {
        return add(region.getHash(), 0, ByteBuffer.wrap(region.toBytes()));
    }

    public boolean add(SHAHash hash, int compression, ByteBuffer data) throws IOException {
        if (!known.add(hash)) return false;
        write(hash, compression, data);
        return true;
    }

    public int getBlobsWritten() {
        return blobsWritten;
    }

    public long getBytesWritten() {
        return bytesWritten;
    }

    public int getPackCount() {
        return packCount;
    }

    public int getBlobsDeduplicated() {
        return blobsDeduplicated;
    }

    public long getBytesDeduplicated() {
        return bytesDeduplicated;
    }

    @Override
    public void close() throws IOException {
        finishPack();
    }

    /** Seals the current pack, if any. The index is written before the pack is moved into place. */
    private void finishPack() throws IOException {
        if (channel == null) return; // nothing new to store

        channel.force(true);
        channel.close();
        channel = null;

        File pack = new File(directory, name + ".pack");
        PackIndex.write(new File(directory, name + PackIndex.EXTENSION), entries);
        Files.move(tempPack.toPath(), pack.toPath(), StandardCopyOption.REPLACE_EXISTING);

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
