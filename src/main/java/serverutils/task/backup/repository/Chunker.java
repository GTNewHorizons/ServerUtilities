package serverutils.task.backup.repository;

import java.io.IOException;

/**
 * Content-defined chunking with a Gear rolling hash. Boundaries depend only on the last 64 bytes, so an insertion or a
 * rewritten region sector only changes the chunks around it and everything else still deduplicates, like Borg's buzhash
 * chunker.
 */
final class Chunker implements Splitter {

    interface Sink {

        /** The buffer is reused once this returns. */
        void accept(byte[] data, int length) throws IOException;
    }

    // Fixed forever: changing it would stop new chunks from deduplicating against existing repositories.
    private static final long[] GEAR = gearTable(0x5355_4348_554E_4B31L);

    private final int minSize;
    private final int maxSize;
    private final int shift;
    private final byte[] buffer;
    private final Sink sink;
    private int length;
    private long hash;

    Chunker(Params params, Sink sink) {
        this.minSize = params.minSize;
        this.maxSize = params.maxSize;
        this.shift = 64 - params.maskBits;
        this.buffer = new byte[params.maxSize];
        this.sink = sink;
    }

    @Override
    public void update(byte[] data, int offset, int count) throws IOException {
        for (int i = offset, end = offset + count; i < end; i++) {
            byte b = data[i];
            buffer[length++] = b;
            hash = (hash << 1) + GEAR[b & 0xff];
            // The top bits carry the influence of the last 64 bytes; the low bits only of the newest few.
            if (length >= maxSize || (length >= minSize && (hash >>> shift) == 0)) emit();
        }
    }

    @Override
    public void finish() throws IOException {
        if (length > 0) emit();
    }

    private void emit() throws IOException {
        int chunk = length;
        length = 0;
        hash = 0;
        sink.accept(buffer, chunk);
    }

    private static long[] gearTable(long seed) {
        long[] table = new long[256];
        for (int i = 0; i < table.length; i++) {
            // SplitMix64
            long z = (seed += 0x9E3779B97F4A7C15L);
            z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
            z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
            table[i] = z ^ (z >>> 31);
        }
        return table;
    }

    static final class Params {

        /** Average about 80 KiB: region files change a few MC chunks at a time, so smaller chunks dedup better. */
        static final Params DATA = new Params(16 * 1024, 512 * 1024, 16);
        /** Archive item metadata, which is mostly 32-byte chunk ids of unchanged files. */
        static final Params METADATA = new Params(4 * 1024, 64 * 1024, 13);
        /** Region files are cut at Minecraft chunk boundaries instead, but no smaller than this. */
        static final int REGION_MIN_SIZE = 32 * 1024;

        final int minSize;
        final int maxSize;
        final int maskBits;

        Params(int minSize, int maxSize, int maskBits) {
            if (minSize < 64 || maxSize < minSize || maxSize > 64 * 1024 * 1024 || maskBits < 6 || maskBits > 30) {
                throw new IllegalArgumentException(
                        "Invalid chunker parameters " + minSize + "," + maxSize + "," + maskBits);
            }
            this.minSize = minSize;
            this.maxSize = maxSize;
            this.maskBits = maskBits;
        }
    }
}
