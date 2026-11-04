package serverutils.task.backup.repository;

/** One file in an archive: its metadata and the ids of the chunks holding its contents, in order. */
public final class Item {

    public final String path;
    public final long size;
    public final long modified;
    public final int crc;
    final byte[] chunks;

    Item(String path, long size, long modified, int crc, byte[] chunks) {
        if (chunks.length % ChunkIndex.ID_LENGTH != 0) throw new IllegalArgumentException("Invalid chunk list");
        this.path = path;
        this.size = size;
        this.modified = modified;
        this.crc = crc;
        this.chunks = chunks;
    }

    public int chunkCount() {
        return chunks.length / ChunkIndex.ID_LENGTH;
    }
}
