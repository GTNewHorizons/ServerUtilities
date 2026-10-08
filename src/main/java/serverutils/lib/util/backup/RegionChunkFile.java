package serverutils.lib.util.backup;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

public class RegionChunkFile implements ChunkFile {
    private final Path path;

    public static final int HEADER_SIZE = 8192;
    public static final int CHUNK_COUNT = 1024;
    public static final int SECTOR_SIZE = 4096;
    public static final int TIMESTAMP_TABLE_OFFSET = 4096;

    public RegionChunkFile(Path path) {
        this.path = path;
    }

    public FileChannel open() throws IOException {
        return FileChannel.open(path);
    }
}
