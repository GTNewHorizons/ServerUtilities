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

public class PackWriter implements Closeable {

    private final File directory;
    private final Set<String> known;
    private final String name;
    private final File tempPack;
    private final List<PackIndex.Entry> entries = new ArrayList<>();

    private FileChannel channel;
    private long position;
    private int blobsDeduplicated;
    private long bytesDeduplicated;

    public PackWriter(File directory, Set<String> known) {
        this.directory = directory;
        this.known = known;
        this.name = "pack-" + System.currentTimeMillis();
        this.tempPack = new File(directory, name + ".pack.tmp");
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

        if (channel == null) {
            channel = FileChannel.open(
                    tempPack.toPath(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        }

        ChunkBlob.Metadata metadata = blob.metadata();
        ByteBuffer data = blob.data();
        int length = data.remaining();
        while (data.hasRemaining()) {
            channel.write(data);
        }

        entries.add(new PackIndex.Entry(metadata.hash(), position, length, metadata.compressionType()));
        position += length;
        return true;
    }

    public int getBlobsWritten() {
        return entries.size();
    }

    public long getBytesWritten() {
        return position;
    }

    public int getBlobsDeduplicated() {
        return blobsDeduplicated;
    }

    public long getBytesDeduplicated() {
        return bytesDeduplicated;
    }

    @Override
    public void close() throws IOException {
        if (channel == null) return; // nothing new to store

        channel.force(true);
        channel.close();
        channel = null;

        File pack = new File(directory, name + ".pack");
        PackIndex.write(new File(directory, name + PackIndex.EXTENSION), entries);
        Files.move(tempPack.toPath(), pack.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
