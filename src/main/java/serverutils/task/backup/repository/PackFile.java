package serverutils.task.backup.repository;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * Append-only segment of chunk records, similar to Borg's segment files. Records are self-describing, so the chunk
 * index can always be rebuilt by scanning the packs.
 *
 * <pre>
 * pack   = MAGIC record*
 * record = int RECORD_MAGIC, byte[32] id, byte method, int rawLength, int storedLength, int crc32(stored), stored
 * </pre>
 */
final class PackFile {

    static final byte[] MAGIC = { 'S', 'U', 'P', 'A', 'C', 'K', 0, 1 };
    static final int RECORD_MAGIC = 0x53554331;
    static final int RECORD_HEADER = 4 + ChunkIndex.ID_LENGTH + 1 + 4 + 4 + 4;
    static final byte STORED = 0;
    static final byte DEFLATED = 1;
    /** Packs roll over at this size, so compaction never has to rewrite more than this per pack. */
    static final long TARGET_SIZE = 64L * 1024 * 1024;

    private PackFile() {}

    static String name(int pack) {
        return String.format("%08x.pack", pack);
    }

    /** Returns the pack id of a pack file name, or -1. */
    static int parseName(String name) {
        if (name.length() != 13 || !name.endsWith(".pack")) return -1;
        try {
            int id = (int) Long.parseLong(name.substring(0, 8), 16);
            return id > 0 ? id : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    static final class Writer implements AutoCloseable {

        final int pack;
        final File file;
        private final FileOutputStream stream;
        private final DataOutputStream output;
        private long position;

        Writer(File file, int pack) throws IOException {
            this.pack = pack;
            this.file = file;
            if (file.exists()) throw new IOException("Pack already exists: " + file);
            stream = new FileOutputStream(file);
            output = new DataOutputStream(new BufferedOutputStream(stream, 1024 * 1024));
            output.write(MAGIC);
            position = MAGIC.length;
        }

        long position() {
            return position;
        }

        /** Appends a record and returns its offset. */
        int append(byte[] id, byte method, int rawLength, byte[] stored, int storedLength) throws IOException {
            CRC32 crc = new CRC32();
            crc.update(stored, 0, storedLength);
            return append(id, method, rawLength, stored, storedLength, (int) crc.getValue());
        }

        int append(byte[] id, byte method, int rawLength, byte[] stored, int storedLength, int crc) throws IOException {
            long offset = position;
            if (offset + RECORD_HEADER + storedLength > Integer.MAX_VALUE) throw new IOException("Pack too large");
            output.writeInt(RECORD_MAGIC);
            output.write(id, 0, ChunkIndex.ID_LENGTH);
            output.writeByte(method);
            output.writeInt(rawLength);
            output.writeInt(storedLength);
            output.writeInt(crc);
            output.write(stored, 0, storedLength);
            position += RECORD_HEADER + storedLength;
            return (int) offset;
        }

        /** Flushes and syncs everything appended so far. */
        void sync() throws IOException {
            output.flush();
            // FileDescriptor.sync, unlike FileChannel.force, does not close the file when the thread is interrupted.
            stream.getFD().sync();
        }

        @Override
        public void close() throws IOException {
            try {
                sync();
            } finally {
                output.close();
            }
        }
    }

    /** One stored record, verified against its checksum. */
    static final class Record {

        final byte[] id = new byte[ChunkIndex.ID_LENGTH];
        byte method;
        int rawLength;
        int storedLength;
        int crc;
        byte[] stored;
    }

    /** Reads the record at the offset and verifies the stored bytes' CRC, but not the chunk hash. */
    static Record read(RandomAccessFile file, long offset, int expectedStoredLength) throws IOException {
        byte[] header = new byte[RECORD_HEADER];
        file.seek(offset);
        file.readFully(header);
        Record record = parseHeader(header);
        if (record.storedLength != expectedStoredLength) throw new IOException("Pack record length mismatch");
        record.stored = new byte[record.storedLength];
        file.readFully(record.stored);
        verify(record);
        return record;
    }

    private static Record parseHeader(byte[] header) throws IOException {
        DataInputStream input = new DataInputStream(new java.io.ByteArrayInputStream(header));
        if (input.readInt() != RECORD_MAGIC) throw new IOException("Bad pack record magic");
        Record record = new Record();
        input.readFully(record.id);
        record.method = input.readByte();
        record.rawLength = input.readInt();
        record.storedLength = input.readInt();
        record.crc = input.readInt();
        if ((record.method != STORED && record.method != DEFLATED) || record.rawLength < 0
                || record.storedLength < 0
                || (record.method == STORED && record.rawLength != record.storedLength)) {
            throw new IOException("Bad pack record header");
        }
        return record;
    }

    private static void verify(Record record) throws IOException {
        CRC32 crc = new CRC32();
        crc.update(record.stored);
        if ((int) crc.getValue() != record.crc) throw new IOException("Pack record checksum mismatch");
    }

    interface ScanVisitor {

        void record(Record record, int offset) throws IOException;
    }

    /**
     * Visits every intact record up to the first damaged one. A crash while a backup was writing leaves such a torn
     * tail; the records before it are complete.
     *
     * @return the number of bytes holding intact records, less than the file length if the scan stopped early
     */
    static long scan(File file, ScanVisitor visitor) throws IOException {
        try (InputStream raw = new BufferedInputStream(new FileInputStream(file), 1024 * 1024)) {
            DataInputStream input = new DataInputStream(raw);
            byte[] magic = new byte[MAGIC.length];
            try {
                input.readFully(magic);
            } catch (EOFException e) {
                return 0;
            }
            if (!Arrays.equals(magic, MAGIC)) throw new IOException("Not a pack file: " + file);
            long position = MAGIC.length;
            long limit = file.length();
            byte[] header = new byte[RECORD_HEADER];
            while (true) {
                if (Thread.currentThread().isInterrupted()) throw new java.io.InterruptedIOException("Cancelled");
                Record record;
                try {
                    input.readFully(header);
                } catch (EOFException e) {
                    return position;
                }
                try {
                    record = parseHeader(header);
                } catch (IOException e) {
                    return position;
                }
                long end = position + RECORD_HEADER + record.storedLength;
                if (end > limit) return position;
                record.stored = new byte[record.storedLength];
                input.readFully(record.stored);
                try {
                    verify(record);
                } catch (IOException e) {
                    return position;
                }
                visitor.record(record, (int) position);
                position = end;
            }
        }
    }
}
