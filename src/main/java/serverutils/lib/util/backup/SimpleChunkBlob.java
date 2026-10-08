package serverutils.lib.util.backup;

import com.github.bsideup.jabel.Desugar;
import java.nio.ByteBuffer;

@Desugar
public record SimpleChunkBlob(Metadata metadata, ByteBuffer data) implements ChunkBlob {
    @Override
    public Metadata metadata() {
        return metadata;
    }

    @Override
    public ByteBuffer data() {
        return data.asReadOnlyBuffer();
    }
}
