package serverutils.lib.util.backup;

import static serverutils.lib.util.backup.RegionChunkFile.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class SimpleChunkReader implements ChunkReader {

    @Override
    public List<ChunkBlob> readChunks(ChunkFile chunkFile) throws IOException {
        // abstraction used for possible support of different chunk storage formats
        if (!(chunkFile instanceof RegionChunkFile rcf)) {
            throw new IllegalArgumentException("ChunkFile must be an instance of RegionChunkFile");
        }

        try (FileChannel channel = rcf.open()) {
            ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
            channel.read(header);
            header.flip();

            List<ChunkBlob> blobs = new ArrayList<>();
            for (int i = 0; i < CHUNK_COUNT; i++) {
                int loc = header.getInt(i * 4);
                if (loc == 0) continue;

                long offset = (long) (loc >>> 8) * SECTOR_SIZE;
                long timestamp = Integer.toUnsignedLong(header.getInt(TIMESTAMP_TABLE_OFFSET + i * 4));

                ByteBuffer lenBuf = ByteBuffer.allocate(5);
                channel.read(lenBuf, offset);
                lenBuf.flip();

                int len = lenBuf.getInt();
                int compression = len & 0xFF;

                if (len <= 0 || len > (loc & 0xFF) * SECTOR_SIZE) continue; // corrupt

                // strip external flag (n/a on 1.7.10)
                compression &= 0x7F;

                ByteBuffer data = ByteBuffer.allocate(len);
                channel.read(data, offset + 4);
                data.flip();

                byte[] hash = Hasher.hashChunk(compression, data.array());
                ChunkBlob.Metadata metadata = new ChunkBlob.Metadata(i, timestamp, compression, hash);

                SimpleChunkBlob blob = new SimpleChunkBlob(metadata, data);
                blobs.add(blob);
            }
            return blobs;
        }
    }
}
