package serverutils.lib.util.backup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Binary format for the region manifest used in incremental backups
 * <p>
 * Layout: "MREG" [4] COUNT [4] -- Per Chunk -- INDEX [2] TIMESTAMP [4] HASH [32]
 *
 */
public final class RegionManifest {

    public static final String MAGIC = "MREG";
    private static final int HASH_SIZE = 32;
    private static final int ENTRY_SIZE = 2 + 4 + HASH_SIZE;

    private RegionManifest() {}

    public static byte[] encode(Collection<ChunkBlob.Metadata> chunks) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(6 + chunks.size() * ENTRY_SIZE);
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeBytes(MAGIC);
            out.writeShort(chunks.size());

            for (ChunkBlob.Metadata metadata : chunks) {
                out.writeShort(metadata.index());
                // timestamp is a u32 stored in a long, this conversion does not truncate
                out.writeInt((int) metadata.timestamp());
                out.write(metadata.hash().getBytes());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static NavigableMap<Integer, ChunkBlob.Metadata> decode(byte[] data) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            byte[] magic = new byte[MAGIC.length()];
            in.readFully(magic);
            if (!MAGIC.equals(new String(magic, StandardCharsets.US_ASCII))) {
                throw new IOException("Invalid region manifest magic");
            }

            int count = in.readUnsignedShort();
            NavigableMap<Integer, ChunkBlob.Metadata> chunks = new TreeMap<>();
            for (int i = 0; i < count; i++) {
                int index = in.readUnsignedShort();
                long timestamp = Integer.toUnsignedLong(in.readInt());
                byte[] hash = new byte[HASH_SIZE];
                in.readFully(hash);
                chunks.put(index, new ChunkBlob.Metadata(index, timestamp, 0, new SHAHash(hash)));
            }
            return chunks;
        }
    }
}
