package serverutils.lib.util.backup;

import static serverutils.lib.util.backup.RegionChunkFile.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class SimpleChunkWriter implements ChunkWriter {

    private static final int MAX_SECTORS = 255;

    @Override
    public void write(ChunkFile chunkFile, List<ChunkBlob> blobs) throws IOException {
        // abstraction used for possible support of different chunk storage formats
        if (!(chunkFile instanceof RegionChunkFile rcf)) {
            throw new IllegalArgumentException("ChunkFile must be an instance of RegionChunkFile");
        }

        try (FileChannel channel = rcf
                .open(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
            long position = HEADER_SIZE;
            int sector = HEADER_SIZE / SECTOR_SIZE;

            for (ChunkBlob blob : blobs) {
                ChunkBlob.Metadata metadata = blob.metadata();
                int index = metadata.index();
                if (index < 0 || index >= CHUNK_COUNT) {
                    throw new IllegalArgumentException("Invalid chunk index " + index);
                }
                if (header.getInt(index * 4) != 0) {
                    throw new IllegalArgumentException("Chunk " + index + " was given more than once");
                }

                ByteBuffer data = blob.data();
                int length = data.remaining();
                // blobs that only reference an earlier snapshot carry no data, those can't be written back
                if (length == 0) throw new IllegalArgumentException("Chunk " + index + " has no data");

                int sectors = (length + 4 + SECTOR_SIZE - 1) / SECTOR_SIZE;
                if (sectors > MAX_SECTORS) {
                    throw new IllegalArgumentException("Chunk " + index + " is too large for a region file");
                }

                ByteBuffer padded = ByteBuffer.allocate(sectors * SECTOR_SIZE);
                padded.putInt(length);
                padded.put(data);
                padded.clear();
                writeFully(channel, padded, position);

                header.putInt(index * 4, (sector << 8) | sectors);
                // timestamp is a u32 stored in a long, this conversion does not truncate
                header.putInt(TIMESTAMP_TABLE_OFFSET + index * 4, (int) metadata.timestamp());

                position += (long) sectors * SECTOR_SIZE;
                sector += sectors;
            }

            // the header goes last so an interrupted write never points at chunks that are not there
            channel.write(header);
            channel.force(true);
        }
    }

    private static void writeFully(FileChannel channel, ByteBuffer buffer, long position) throws IOException {
        while (buffer.hasRemaining()) {
            position += channel.write(buffer, position);
        }
    }
}
