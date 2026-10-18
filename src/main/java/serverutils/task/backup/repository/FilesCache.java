package serverutils.task.backup.repository;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Remembers the chunks of files by their size, modification time and file key, so files that have not changed since the
 * last backup are not read again. Like Borg's files cache, it is only a cache: entries are checked against the chunk
 * index before use, and dropped when not seen for {@link #TTL} backups.
 */
final class FilesCache {

    private static final int MAGIC = 0x53554643;
    private static final int TTL = 10;
    /** Timestamps on coarse filesystems may not change for writes this soon after the file was read. */
    static final long TIMESTAMP_MARGIN_MILLIS = 2000;

    private static final class Entry {

        final String source;
        final long size;
        final long modifiedNanos;
        final String key;
        int age;
        final Item item;

        Entry(String source, long size, long modifiedNanos, String key, int age, Item item) {
            this.source = source;
            this.size = size;
            this.modifiedNanos = modifiedNanos;
            this.key = key;
            this.age = age;
            this.item = item;
        }
    }

    private final Map<String, Entry> entries = new HashMap<>();
    private final Set<String> seen = new HashSet<>();

    static FilesCache load(File file) {
        FilesCache cache = new FilesCache();
        if (!file.isFile()) return cache;
        try (DataInputStream input = new DataInputStream(
                new BufferedInputStream(new GZIPInputStream(new FileInputStream(file)), 64 * 1024))) {
            if (input.readInt() != MAGIC) return cache;
            int count = input.readInt();
            for (int i = 0; i < count; i++) {
                String path = input.readUTF();
                String source = input.readUTF();
                long size = input.readLong();
                long modifiedNanos = input.readLong();
                String key = input.readUTF();
                int age = input.readInt();
                long modified = input.readLong();
                int crc = input.readInt();
                int chunks = input.readInt();
                if (chunks < 0 || chunks > Integer.MAX_VALUE / ChunkIndex.ID_LENGTH) throw new IOException();
                byte[] ids = new byte[chunks * ChunkIndex.ID_LENGTH];
                input.readFully(ids);
                cache.entries.put(
                        path,
                        new Entry(source, size, modifiedNanos, key, age, new Item(path, size, modified, crc, ids)));
            }
        } catch (IOException | RuntimeException e) {
            // A damaged cache only costs reading every file once.
            cache.entries.clear();
        }
        return cache;
    }

    static String key(BasicFileAttributes attributes) {
        Object key = attributes.fileKey();
        return key == null ? "" : key.toString();
    }

    static long modifiedNanos(BasicFileAttributes attributes) {
        return attributes.lastModifiedTime().to(TimeUnit.NANOSECONDS);
    }

    /** Returns the stored item if the file is unchanged and all its chunks are still in the repository. */
    Item lookup(String path, String source, BasicFileAttributes attributes, ChunkIndex index) {
        Entry entry = entries.get(path);
        if (entry == null || !entry.source.equals(source)
                || entry.size != attributes.size()
                || entry.modifiedNanos != modifiedNanos(attributes)
                || !entry.key.equals(key(attributes))) {
            return null;
        }
        byte[] chunks = entry.item.chunks;
        for (int offset = 0; offset < chunks.length; offset += ChunkIndex.ID_LENGTH) {
            if (index.find(chunks, offset) < 0) return null;
        }
        seen.add(path);
        entry.age = 0;
        return entry.item;
    }

    void put(String path, String source, BasicFileAttributes attributes, Item item) {
        entries.put(path, new Entry(source, attributes.size(), modifiedNanos(attributes), key(attributes), 0, item));
        seen.add(path);
    }

    void save(File file) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(new GZIPOutputStream(buffer, 64 * 1024))) {
            for (Iterator<Map.Entry<String, Entry>> it = entries.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, Entry> entry = it.next();
                if (!seen.contains(entry.getKey()) && ++entry.getValue().age > TTL) it.remove();
            }
            output.writeInt(MAGIC);
            output.writeInt(entries.size());
            for (Map.Entry<String, Entry> pair : entries.entrySet()) {
                Entry entry = pair.getValue();
                output.writeUTF(pair.getKey());
                output.writeUTF(entry.source);
                output.writeLong(entry.size);
                output.writeLong(entry.modifiedNanos);
                output.writeUTF(entry.key);
                output.writeInt(entry.age);
                output.writeLong(entry.item.modified);
                output.writeInt(entry.item.crc);
                output.writeInt(entry.item.chunkCount());
                output.write(entry.item.chunks);
            }
        }
        Repository.writeAtomic(file, buffer.toByteArray());
    }
}
