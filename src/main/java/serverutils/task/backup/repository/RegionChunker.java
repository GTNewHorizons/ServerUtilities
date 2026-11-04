package serverutils.task.backup.repository;

import java.io.IOException;
import java.util.TreeSet;

/**
 * Splits Anvil region files where Minecraft's own chunks start and end. Minecraft rewrites a changed chunk in place (or
 * moves it to free sectors) and leaves the rest of the file alone, so cutting on those sector boundaries keeps the
 * unchanged chunks deduplicated far better than content-defined boundaries, which fall at random offsets. The file is
 * still stored byte for byte; nothing is decoded.
 *
 * <pre>
 * 0x0000 location table: 1024 x (3-byte sector offset, 1-byte sector count)
 * 0x1000 timestamp table, rewritten on every save
 * 0x2000 chunk data in 4 KiB sectors
 * </pre>
 */
final class RegionChunker implements Splitter {

    static final int SECTOR = 4096;
    static final int HEADER = 2 * SECTOR;

    private final int minSize;
    private final int maxSize;
    private final Chunker.Sink sink;
    private final byte[] buffer;
    private final byte[] locations = new byte[SECTOR];
    private int length;
    private long position;
    private int[] boundaries = new int[0];
    private int next;

    RegionChunker(int minSize, int maxSize, Chunker.Sink sink) {
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.sink = sink;
        this.buffer = new byte[Math.max(maxSize, HEADER)];
    }

    static boolean isRegion(String path) {
        return path.endsWith(".mca") || path.endsWith(".mcr");
    }

    @Override
    public void update(byte[] data, int offset, int count) throws IOException {
        while (count > 0) {
            // Copy up to the next sector boundary at once; cuts only ever happen on one.
            int toSector = SECTOR - (int) (position % SECTOR);
            int step = Math.min(Math.min(count, toSector), buffer.length - length);
            System.arraycopy(data, offset, buffer, length, step);
            length += step;
            position += step;
            offset += step;
            count -= step;
            if (position % SECTOR == 0) atSector();
            else if (length == buffer.length) emit();
        }
    }

    private void atSector() throws IOException {
        if (position <= HEADER) {
            if (position == SECTOR) System.arraycopy(buffer, 0, locations, 0, SECTOR);
            // The location table and the timestamp table change independently.
            emit();
            if (position == HEADER) parseHeader();
            return;
        }
        while (next < boundaries.length && boundaries[next] < position) next++;
        boolean boundary = next < boundaries.length && boundaries[next] == position;
        if (length >= maxSize || (boundary && length >= minSize)) emit();
    }

    private void parseHeader() {
        TreeSet<Integer> offsets = new TreeSet<>();
        for (int i = 0; i < 1024; i++) {
            int entry = (locations[i * 4] & 0xff) << 24 | (locations[i * 4 + 1] & 0xff) << 16
                    | (locations[i * 4 + 2] & 0xff) << 8
                    | (locations[i * 4 + 3] & 0xff);
            long sector = entry >>> 8;
            int count = entry & 0xff;
            if (sector < 2 || count == 0) continue;
            long start = sector * SECTOR, end = (sector + count) * SECTOR;
            if (end > Integer.MAX_VALUE) continue;
            offsets.add((int) start);
            // Free sectors after a chunk belong to whatever reuses them next, not to this chunk.
            offsets.add((int) end);
        }
        boundaries = offsets.stream().mapToInt(Integer::intValue).toArray();
    }

    private void emit() throws IOException {
        if (length == 0) return;
        int chunk = length;
        length = 0;
        sink.accept(buffer, chunk);
    }

    @Override
    public void finish() throws IOException {
        emit();
    }
}
