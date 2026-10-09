package serverutils.lib.util.backup;

import java.io.IOException;
import java.util.List;

public interface ChunkReader {

    List<ChunkBlob> readChunks(ChunkFile chunkFile) throws IOException;
}
