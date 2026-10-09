package serverutils.lib.util.backup;

import java.io.IOException;
import java.util.List;

public interface ChunkWriter {
    void write(ChunkFile file, List<ChunkBlob> blobs) throws IOException;
}
