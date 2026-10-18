package serverutils.task.backup.repository;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * Adds one archive to a repository. New chunks go to fresh pack files, so nothing an existing archive needs is ever
 * modified. Committing syncs the packs and the index before the archive file is written, so a crash at any point leaves
 * at most unreferenced chunks, which later backups reuse or compaction removes.
 */
public final class RepositoryWriter implements Closeable {

    private final Repository repository;
    private final int compressionLevel;
    private final Deflater deflater;
    private final MessageDigest sha256 = Repository.sha256();
    private final FilesCache filesCache;
    private final List<Item> items = new ArrayList<>();
    private final Set<String> paths = new HashSet<>();
    private final byte[] readBuffer = new byte[64 * 1024];
    private byte[] compressed = new byte[0];
    private PackFile.Writer pack;
    private boolean changed;
    private boolean committed;

    /** Bytes of new chunks after compression, including record headers. */
    public long storedBytes;
    public long newChunks;
    public long reusedChunks;
    public long readBytes;
    public int unchangedFiles;

    RepositoryWriter(Repository repository, int compressionLevel) {
        this.repository = repository;
        this.compressionLevel = compressionLevel;
        this.deflater = compressionLevel > 0 ? new Deflater(compressionLevel) : null;
        this.filesCache = FilesCache.load(filesCacheFile());
    }

    private File filesCacheFile() {
        return new File(repository.root, "files-cache");
    }

    /** Adds a file, reusing its chunks without reading it if the files cache says it is unchanged. */
    public void addFile(String path, File file) throws IOException {
        claim(path);
        BasicFileAttributes before = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        String source = file.getAbsoluteFile().toPath().normalize().toString();
        Item cached = filesCache.lookup(path, source, before, repository.index);
        if (cached != null) {
            items.add(cached);
            unchangedFiles++;
            reusedChunks += cached.chunkCount();
            return;
        }
        long readStart = System.currentTimeMillis();
        Item item;
        try (InputStream input = new FileInputStream(file)) {
            item = store(path, before.lastModifiedTime().toMillis(), input, -1);
        }
        items.add(item);
        BasicFileAttributes after = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        // Only remember files that provably did not change while, or just after, being read.
        if (item.size == before.size() && after.size() == before.size()
                && FilesCache.modifiedNanos(after) == FilesCache.modifiedNanos(before)
                && FilesCache.key(after).equals(FilesCache.key(before))
                && before.lastModifiedTime().toMillis() < readStart - FilesCache.TIMESTAMP_MARGIN_MILLIS) {
            filesCache.put(path, source, before, item);
        }
    }

    /** Adds exactly {@code size} bytes from the stream, which must match the CRC. The stream is not closed. */
    public void addStream(String path, long modified, InputStream input, long size, int crc) throws IOException {
        if (size < 0) throw new IOException("Missing size for " + path);
        claim(path);
        Item item = store(path, modified, input, size);
        if (item.crc != crc) throw new IOException("Checksum mismatch while storing " + path);
        items.add(item);
    }

    private void claim(String path) throws IOException {
        if (committed) throw new IllegalStateException("Archive already committed");
        if (path.isEmpty() || path.length() > 4096) throw new IOException("Invalid path: " + path);
        if (!paths.add(path)) throw new IOException("Duplicate path in archive: " + path);
    }

    private Item store(String path, long modified, InputStream input, long exactSize) throws IOException {
        ByteArrayOutputStream ids = new ByteArrayOutputStream();
        Chunker.Sink sink = (data, length) -> ids.write(storeChunk(data, length));
        Splitter chunker = RegionChunker.isRegion(path)
                ? new RegionChunker(Chunker.Params.REGION_MIN_SIZE, repository.chunkerParams.maxSize, sink)
                : new Chunker(repository.chunkerParams, sink);
        CRC32 crc = new CRC32();
        long size = 0;
        while (exactSize < 0 || size < exactSize) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Backup cancelled");
            int wanted = exactSize < 0 ? readBuffer.length : (int) Math.min(readBuffer.length, exactSize - size);
            int read = input.read(readBuffer, 0, wanted);
            if (read < 0) {
                if (exactSize >= 0) throw new EOFException("Truncated input for " + path);
                break;
            }
            crc.update(readBuffer, 0, read);
            chunker.update(readBuffer, 0, read);
            size += read;
        }
        chunker.finish();
        readBytes += size;
        return new Item(path, size, modified, (int) crc.getValue(), ids.toByteArray());
    }

    /** Stores a chunk unless the repository already has it, and returns its id. */
    private byte[] storeChunk(byte[] data, int length) throws IOException {
        sha256.update(data, 0, length);
        byte[] id = sha256.digest();
        if (repository.index.find(id, 0) >= 0) {
            reusedChunks++;
            return id;
        }
        byte method = PackFile.STORED;
        byte[] stored = data;
        int storedLength = length;
        if (deflater != null) {
            if (compressed.length < length) compressed = new byte[length];
            deflater.reset();
            deflater.setInput(data, 0, length);
            deflater.finish();
            int size = 0;
            while (!deflater.finished() && size < compressed.length) {
                size += deflater.deflate(compressed, size, compressed.length - size);
            }
            // Already compressed data, such as region chunks, is kept as is when deflating does not shrink it.
            if (deflater.finished() && size < length) {
                method = PackFile.DEFLATED;
                stored = compressed;
                storedLength = size;
            }
        }
        if (pack == null || pack.position() >= PackFile.TARGET_SIZE) rollPack();
        int offset = pack.append(id, method, length, stored, storedLength);
        repository.index.put(id, 0, pack.pack, offset, storedLength, length);
        newChunks++;
        storedBytes += PackFile.RECORD_HEADER + storedLength;
        changed = true;
        return id;
    }

    private void rollPack() throws IOException {
        finishPack();
        int id = repository.allocatePack();
        pack = new PackFile.Writer(new File(repository.data, PackFile.name(id)), id);
    }

    private void finishPack() throws IOException {
        if (pack == null) return;
        PackFile.Writer finished = pack;
        pack = null;
        finished.close();
        repository.packLengths.put(finished.pack, finished.file.length());
    }

    /**
     * Stores the file list and writes the archive to {@code output}, which the caller moves into place. The header
     * should hold {@code createdAt} and {@code customName}, and may hold {@code worldId} and {@code worldName}.
     */
    public ArchiveInfo commit(Properties header, File output) throws IOException {
        if (committed) throw new IllegalStateException("Archive already committed");
        items.sort((a, b) -> a.path.compareTo(b.path));
        long totalSize = 0;
        ByteArrayOutputStream metadataIds = new ByteArrayOutputStream();
        Chunker chunker = new Chunker(
                Chunker.Params.METADATA,
                (data, length) -> metadataIds.write(storeChunk(data, length)));
        try (DataOutputStream stream = new DataOutputStream(
                new BufferedOutputStream(new ChunkerOutput(chunker), 64 * 1024))) {
            stream.writeInt(Repository.ITEMS_MAGIC);
            stream.writeInt(1);
            for (Item item : items) {
                stream.writeByte(1);
                stream.writeUTF(item.path);
                stream.writeLong(item.size);
                stream.writeLong(item.modified);
                stream.writeInt(item.crc);
                stream.writeInt(item.chunkCount());
                stream.write(item.chunks);
                totalSize += item.size;
            }
            stream.writeByte(0);
        }
        Properties full = new Properties();
        full.putAll(header);
        full.setProperty("version", "1");
        full.setProperty("files", Integer.toString(items.size()));
        full.setProperty("size", Long.toString(totalSize));
        String worldPath = worldPath(header.getProperty("worldName"));
        if (worldPath != null) full.setProperty("worldPath", worldPath);
        else full.remove("worldPath");

        // Everything the archive references must be durable before the archive exists.
        finishPack();
        if (changed) repository.saveIndex();
        changed = false;
        filesCache.save(filesCacheFile());
        byte[] encoded = Repository.encodeArchive(full, metadataIds.toByteArray());
        Repository.writeSynced(output, encoded);
        committed = true;
        return new ArchiveInfo(output.getName(), output, full, metadataIds.toByteArray());
    }

    /** The folder holding level.dat, preferring one named after the world, or null if there is not exactly one. */
    private String worldPath(String worldName) {
        Set<String> all = new HashSet<>();
        Set<String> named = new HashSet<>();
        for (Item item : items) {
            int separator = item.path.lastIndexOf('/');
            if (separator < 0 || item.size <= 0) continue;
            String file = item.path.substring(separator + 1);
            if (!file.equals("level.dat") && !file.equals("level.dat_old")) continue;
            String folder = item.path.substring(0, separator + 1);
            all.add(folder);
            if (worldName != null && (folder.equals(worldName + "/") || folder.endsWith("/" + worldName + "/"))) {
                named.add(folder);
            }
        }
        Set<String> candidates = named.isEmpty() ? all : named;
        return candidates.size() == 1 ? candidates.iterator().next() : null;
    }

    /** Syncs and indexes chunks written so far even without a commit, so an aborted backup's work is reused. */
    @Override
    public void close() throws IOException {
        try {
            finishPack();
            if (changed) repository.saveIndex();
        } finally {
            if (deflater != null) deflater.end();
        }
    }

    private static final class ChunkerOutput extends OutputStream {

        private final Chunker chunker;
        private final byte[] single = new byte[1];

        ChunkerOutput(Chunker chunker) {
            this.chunker = chunker;
        }

        @Override
        public void write(int b) throws IOException {
            single[0] = (byte) b;
            chunker.update(single, 0, 1);
        }

        @Override
        public void write(byte[] data, int offset, int length) throws IOException {
            chunker.update(data, offset, length);
        }

        @Override
        public void close() throws IOException {
            chunker.finish();
        }
    }
}
