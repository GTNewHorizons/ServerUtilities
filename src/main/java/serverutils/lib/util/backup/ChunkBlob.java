package serverutils.lib.util.backup;

import java.nio.ByteBuffer;

import org.apache.commons.codec.binary.Hex;

import com.github.bsideup.jabel.Desugar;

public interface ChunkBlob {

    Metadata metadata();

    ByteBuffer data();

    default String getHash() {
        return metadata().hexHash();
    }

    @Desugar
    record Metadata(int index, long timestamp, int compressionType, byte[] hash) {

        public String hexHash() {
            return Hex.encodeHexString(hash);
        }
    }

    @Desugar
    record Empty(Metadata metadata) implements ChunkBlob {

        private static final ByteBuffer EMPTY = ByteBuffer.allocate(0);

        @Override
        public ByteBuffer data() {
            return EMPTY;
        }
    }
}
