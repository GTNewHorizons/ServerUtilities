package serverutils.lib.util.backup;

import java.nio.ByteBuffer;

import com.github.bsideup.jabel.Desugar;

public interface ChunkBlob extends Hashable {

    Metadata metadata();

    ByteBuffer data();

    @Override
    default SHAHash getHash() {
        return metadata().hash;
    }

    @Desugar
    record Metadata(int index, long timestamp, int compressionType, SHAHash hash) {

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
