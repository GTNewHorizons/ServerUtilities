package serverutils.task.backup.repository;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * A deduplicating backup repository modelled on Borg. Files are split into content-defined chunks, each chunk is stored
 * once under its SHA-256 id in append-only pack files, and an archive is a small file listing the chunks of its file
 * list, which is itself chunked so unchanged files cost almost nothing.
 *
 * <pre>
 * config            repository format and chunker parameters
 * lock              held shared by readers and exclusively by writers
 * data/*.pack       chunk records
 * archives/*.archive one per backup
 * index             chunk id to pack location; a cache, rebuilt from the packs when missing or stale
 * files-cache       size and timestamps of files already stored, so unchanged files are not read again
 * </pre>
 *
 * This class only uses the JDK, so the repository tool can run without Minecraft on the classpath.
 */
public final class Repository implements Closeable {

    public static final String ARCHIVE_EXTENSION = ".archive";
    static final String FORMAT = "serverutilities-backup-repository";
    private static final String VERSION = "1";
    private static final byte[] INDEX_MAGIC = { 'S', 'U', 'I', 'D', 'X', 0, 0, 1 };
    private static final byte[] ARCHIVE_MAGIC = { 'S', 'U', 'A', 'R', 'C', 0, 0, 1 };
    static final int ITEMS_MAGIC = 0x53554954;
    private static final int MAX_ARCHIVE_FILE = 64 * 1024 * 1024;
    private static final int MAX_READERS = 16;

    public final File root;
    final File data;
    final File archives;
    final boolean writable;
    final Chunker.Params chunkerParams;
    private final RandomAccessFile lockFile;
    private final FileLock lock;
    ChunkIndex index = new ChunkIndex();
    /** Packs covered by the index, with the length that was indexed. */
    final Map<Integer, Long> packLengths = new TreeMap<>();
    private int nextPack = 1;
    private final List<String> warnings = new ArrayList<>();
    private final Inflater inflater = new Inflater();
    private final MessageDigest sha256 = sha256();
    private final Map<Integer, RandomAccessFile> readers = new LinkedHashMap<>(16, 0.75f, true);

    private Repository(File root, boolean writable, long lockTimeout) throws IOException {
        this.root = root;
        this.data = new File(root, "data");
        this.archives = new File(root, "archives");
        this.writable = writable;
        Properties config = new Properties();
        File configFile = new File(root, "config");
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(configFile),
                StandardCharsets.UTF_8)) {
            config.load(reader);
        }
        if (!FORMAT.equals(config.getProperty("format")) || !VERSION.equals(config.getProperty("version"))) {
            throw new IOException("Unsupported backup repository format in " + root);
        }
        String[] chunker = config.getProperty("chunker", "").split(",");
        if (chunker.length != 3) throw new IOException("Invalid chunker parameters in " + configFile);
        try {
            chunkerParams = new Chunker.Params(
                    Integer.parseInt(chunker[0].trim()),
                    Integer.parseInt(chunker[1].trim()),
                    Integer.parseInt(chunker[2].trim()));
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid chunker parameters in " + configFile, e);
        }
        lockFile = new RandomAccessFile(new File(root, "lock"), writable ? "rw" : "r");
        try {
            lock = acquireLock(lockFile.getChannel(), !writable, lockTimeout);
            if (writable) deleteTemporaries(root);
            loadIndex();
        } catch (IOException | RuntimeException e) {
            lockFile.close();
            throw e;
        }
    }

    /** Removes staging files a crash left behind. Only safe while holding the exclusive lock. */
    private static void deleteTemporaries(File folder) {
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.getName().startsWith(".su-save-") && file.getName().endsWith(".tmp")
                    && Files.isRegularFile(file.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                file.delete();
            }
        }
    }

    public static boolean exists(File root) {
        return new File(root, "config").isFile();
    }

    /** Opens an existing repository, waiting up to the timeout for other processes using it. */
    public static Repository open(File root, boolean writable, long lockTimeoutMillis) throws IOException {
        if (!exists(root)) throw new IOException("Not a backup repository: " + root.getAbsolutePath());
        return new Repository(root, writable, lockTimeoutMillis);
    }

    public static Repository openOrCreate(File root, long lockTimeoutMillis) throws IOException {
        if (!exists(root)) create(root);
        return open(root, true, lockTimeoutMillis);
    }

    static synchronized void create(File root) throws IOException {
        if (exists(root)) return;
        Files.createDirectories(new File(root, "data").toPath());
        Files.createDirectories(new File(root, "archives").toPath());
        new File(root, "lock").createNewFile();
        Chunker.Params params = Chunker.Params.DATA;
        String config = "# ServerUtilities incremental backup repository. Do not edit.\n" + "format="
                + FORMAT
                + "\nversion="
                + VERSION
                + "\nid="
                + UUID.randomUUID()
                + "\nchunker="
                + params.minSize
                + ","
                + params.maxSize
                + ","
                + params.maskBits
                + "\n";
        writeReadme(root);
        writeAtomic(new File(root, "config"), config.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeReadme(File root) throws IOException {
        String readme = """
                This folder is a ServerUtilities incremental backup repository.
                Files are deduplicated across backups, so the archives here are not usable on their own.

                Stop the server (or restore into a different folder), then use the repository tool in the
                ServerUtilities jar, from the server folder:

                  java -jar mods/ServerUtilities-<version>.jar list
                  java -jar mods/ServerUtilities-<version>.jar restore <archive> <target folder>
                  java -jar mods/ServerUtilities-<version>.jar export <archive> <file.zip>
                  java -jar mods/ServerUtilities-<version>.jar check

                Run it without arguments for every command and option.
                Never delete or edit files in here by hand; deleting one pack file damages every backup using it.
                """;
        Files.write(new File(root, "README.txt").toPath(), readme.getBytes(StandardCharsets.UTF_8));
    }

    private static FileLock acquireLock(FileChannel channel, boolean shared, long timeout) throws IOException {
        long deadline = System.currentTimeMillis() + timeout;
        while (true) {
            FileLock lock;
            try {
                lock = channel.tryLock(0, Long.MAX_VALUE, shared);
            } catch (OverlappingFileLockException e) {
                // Held by this JVM, which serializes its own repository access.
                lock = null;
            }
            if (lock != null) return lock;
            if (System.currentTimeMillis() >= deadline) {
                throw new IOException(
                        "Backup repository is in use by another process (a running backup or repository tool)");
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException("Interrupted while waiting for the backup repository lock");
            }
        }
    }

    /** Problems found while opening, such as a rebuilt index or a damaged pack tail. */
    public List<String> warnings() {
        return Collections.unmodifiableList(warnings);
    }

    void warn(String message) {
        warnings.add(message);
    }

    void requireWritable() throws IOException {
        if (!writable) throw new IOException("Backup repository is open read-only");
    }

    // ---- Chunk index ----

    private Map<Integer, File> listPacks() throws IOException {
        File[] files = data.listFiles();
        if (files == null) throw new IOException("Cannot list " + data);
        Map<Integer, File> packs = new TreeMap<>();
        for (File file : files) {
            int pack = PackFile.parseName(file.getName());
            if (pack > 0 && Files.isRegularFile(file.toPath(), LinkOption.NOFOLLOW_LINKS)) packs.put(pack, file);
        }
        return packs;
    }

    private void loadIndex() throws IOException {
        Map<Integer, File> packs = listPacks();
        File file = new File(root, "index");
        boolean loaded = false;
        if (file.isFile()) {
            try {
                loaded = readIndex(file, packs);
                if (!loaded) warn("Chunk index is out of date; rebuilding it from the pack files");
            } catch (IOException | RuntimeException e) {
                warn("Chunk index is unreadable (" + e.getMessage() + "); rebuilding it from the pack files");
            }
        }
        if (!loaded) {
            index = new ChunkIndex();
            packLengths.clear();
        }
        boolean changed = !loaded;
        for (Map.Entry<Integer, File> pack : packs.entrySet()) {
            nextPack = Math.max(nextPack, pack.getKey() + 1);
            if (packLengths.containsKey(pack.getKey())) continue;
            // A pack the index does not cover: written by a backup that was interrupted before saving the index.
            File packFile = pack.getValue();
            int id = pack.getKey();
            long intact = PackFile.scan(
                    packFile,
                    (record, offset) -> index.put(record.id, 0, id, offset, record.storedLength, record.rawLength));
            long length = packFile.length();
            if (intact < length) {
                warn("Pack " + packFile.getName() + " has " + (length - intact) + " damaged trailing bytes");
            }
            packLengths.put(id, length);
            changed = true;
        }
        if (changed && writable) saveIndex();
    }

    private boolean readIndex(File file, Map<Integer, File> packs) throws IOException {
        MessageDigest digest = sha256();
        try (DigestInputStream digested = new DigestInputStream(
                new BufferedInputStream(new FileInputStream(file), 1024 * 1024),
                digest)) {
            DataInputStream input = new DataInputStream(digested);
            byte[] magic = new byte[INDEX_MAGIC.length];
            input.readFully(magic);
            if (!Arrays.equals(magic, INDEX_MAGIC)) throw new IOException("bad magic");
            int packCount = input.readInt();
            if (packCount < 0) throw new IOException("bad pack count");
            Map<Integer, Long> lengths = new TreeMap<>();
            for (int i = 0; i < packCount; i++) lengths.put(input.readInt(), input.readLong());
            int entries = input.readInt();
            if (entries < 0) throw new IOException("bad entry count");
            ChunkIndex loaded = new ChunkIndex(entries);
            byte[] id = new byte[ChunkIndex.ID_LENGTH];
            for (int i = 0; i < entries; i++) {
                input.readFully(id);
                int pack = input.readInt();
                int offset = input.readInt();
                int stored = input.readInt();
                int raw = input.readInt();
                if (!lengths.containsKey(pack)) throw new IOException("entry for unknown pack");
                loaded.put(id, 0, pack, offset, stored, raw);
            }
            digested.on(false);
            byte[] expected = new byte[32];
            input.readFully(expected);
            if (!MessageDigest.isEqual(expected, digest.digest())) throw new IOException("checksum mismatch");
            for (Map.Entry<Integer, Long> pack : lengths.entrySet()) {
                File packFile = packs.get(pack.getKey());
                // A pack deleted or changed behind the index's back invalidates its locations.
                if (packFile == null || packFile.length() != pack.getValue()) return false;
            }
            index = loaded;
            packLengths.clear();
            packLengths.putAll(lengths);
            return true;
        }
    }

    void saveIndex() throws IOException {
        requireWritable();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(64 + index.size() * 48);
        MessageDigest digest = sha256();
        DataOutputStream output = new DataOutputStream(new DigestOutputStream(buffer, digest));
        output.write(INDEX_MAGIC);
        output.writeInt(packLengths.size());
        for (Map.Entry<Integer, Long> pack : packLengths.entrySet()) {
            output.writeInt(pack.getKey());
            output.writeLong(pack.getValue());
        }
        output.writeInt(index.size());
        byte[] id = new byte[ChunkIndex.ID_LENGTH];
        for (int slot = 0; slot < index.capacity(); slot++) {
            if (!index.isUsed(slot)) continue;
            index.copyId(slot, id, 0);
            output.write(id);
            output.writeInt(index.pack(slot));
            output.writeInt(index.offset(slot));
            output.writeInt(index.storedLength(slot));
            output.writeInt(index.rawLength(slot));
        }
        output.flush();
        buffer.write(digest.digest());
        writeAtomic(new File(root, "index"), buffer.toByteArray());
    }

    int allocatePack() {
        return nextPack++;
    }

    public int chunkCount() {
        return index.size();
    }

    public int packCount() {
        return packLengths.size();
    }

    /** Bytes used by pack files and archives. */
    public long diskSize() {
        long size = 0;
        for (long length : packLengths.values()) size += length;
        File[] files = archives.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(ARCHIVE_EXTENSION)) size += file.length();
            }
        }
        return size;
    }

    // ---- Reading chunks ----

    private RandomAccessFile reader(int pack) throws IOException {
        RandomAccessFile reader = readers.get(pack);
        if (reader != null) return reader;
        if (readers.size() >= MAX_READERS) {
            Iterator<RandomAccessFile> eldest = readers.values().iterator();
            eldest.next().close();
            eldest.remove();
        }
        reader = new RandomAccessFile(new File(data, PackFile.name(pack)), "r");
        readers.put(pack, reader);
        return reader;
    }

    private void closeReader(int pack) {
        RandomAccessFile reader = readers.remove(pack);
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ignored) {}
        }
    }

    /** Reads a chunk and verifies its contents hash to its id. */
    public byte[] readChunk(byte[] id) throws IOException {
        int slot = index.find(id, 0);
        if (slot < 0) throw new IOException("Missing chunk " + hex(id));
        PackFile.Record record;
        try {
            record = PackFile.read(reader(index.pack(slot)), index.offset(slot), index.storedLength(slot));
        } catch (EOFException e) {
            throw new IOException("Truncated pack " + PackFile.name(index.pack(slot)), e);
        }
        if (!Arrays.equals(record.id, id)) throw new IOException("Chunk index points at the wrong record");
        byte[] raw = decode(record);
        if (!Arrays.equals(sha256.digest(raw), id)) throw new IOException("Chunk " + hex(id) + " is corrupt");
        return raw;
    }

    private byte[] decode(PackFile.Record record) throws IOException {
        if (record.method == PackFile.STORED) return record.stored;
        byte[] raw = new byte[record.rawLength];
        inflater.reset();
        inflater.setInput(record.stored);
        try {
            int length = 0;
            while (length < raw.length && !inflater.finished()) {
                int read = inflater.inflate(raw, length, raw.length - length);
                if (read == 0 && (inflater.needsInput() || inflater.needsDictionary())) break;
                length += read;
            }
            if (length != raw.length || !inflater.finished()) throw new IOException("Chunk size mismatch");
        } catch (DataFormatException e) {
            throw new IOException("Corrupt compressed chunk", e);
        }
        return raw;
    }

    /** Streams the concatenated contents of a chunk list, verifying each chunk as it is read. */
    InputStream openChunks(byte[] chunks) {
        return new InputStream() {

            private int next;
            private byte[] current = new byte[0];
            private int position;

            private boolean fill() throws IOException {
                while (position >= current.length) {
                    if (next * ChunkIndex.ID_LENGTH >= chunks.length) return false;
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Cancelled");
                    byte[] id = Arrays
                            .copyOfRange(chunks, next * ChunkIndex.ID_LENGTH, (next + 1) * ChunkIndex.ID_LENGTH);
                    next++;
                    current = readChunk(id);
                    position = 0;
                }
                return true;
            }

            @Override
            public int read() throws IOException {
                return fill() ? current[position++] & 0xff : -1;
            }

            @Override
            public int read(byte[] buffer, int offset, int length) throws IOException {
                if (length == 0) return 0;
                if (!fill()) return -1;
                int count = Math.min(length, current.length - position);
                System.arraycopy(current, position, buffer, offset, count);
                position += count;
                return count;
            }
        };
    }

    /** Streams a file's contents. The caller should compare the size and CRC with the item's. */
    public InputStream openItem(Item item) {
        return openChunks(item.chunks);
    }

    // ---- Archives ----

    public File archiveFile(String name) {
        return new File(archives, name + ARCHIVE_EXTENSION);
    }

    /** Archive names, without extension, sorted by name. */
    public List<String> archiveNames() throws IOException {
        File[] files = archives.listFiles();
        if (files == null) throw new IOException("Cannot list " + archives);
        List<String> names = new ArrayList<>();
        for (File file : files) {
            String name = file.getName();
            if (name.endsWith(ARCHIVE_EXTENSION) && Files.isRegularFile(file.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                names.add(name.substring(0, name.length() - ARCHIVE_EXTENSION.length()));
            }
        }
        Collections.sort(names);
        return names;
    }

    /** Reads an archive of this repository by its file, rejecting files from elsewhere. */
    public ArchiveInfo readInfo(File file) throws IOException {
        if (!file.getAbsoluteFile().getParentFile().equals(archives.getAbsoluteFile())) {
            throw new IOException("Not an archive of this repository: " + file);
        }
        return readArchive(file);
    }

    public ArchiveInfo readInfo(String name) throws IOException {
        return readArchive(archiveFile(name));
    }

    /** Reads an archive file's header without opening its repository. */
    public static ArchiveInfo readArchive(File file) throws IOException {
        if (file.length() > MAX_ARCHIVE_FILE) throw new IOException("Archive file is too large");
        byte[] bytes = Files.readAllBytes(file.toPath());
        if (bytes.length < ARCHIVE_MAGIC.length + 32
                || !Arrays.equals(Arrays.copyOf(bytes, ARCHIVE_MAGIC.length), ARCHIVE_MAGIC)) {
            throw new IOException("Not a backup archive");
        }
        MessageDigest digest = sha256();
        digest.update(bytes, 0, bytes.length - 32);
        if (!MessageDigest.isEqual(digest.digest(), Arrays.copyOfRange(bytes, bytes.length - 32, bytes.length))) {
            throw new IOException("Archive checksum mismatch");
        }
        DataInputStream input = new DataInputStream(
                new ByteArrayInputStream(bytes, ARCHIVE_MAGIC.length, bytes.length - 32 - ARCHIVE_MAGIC.length));
        byte[] headerBytes = new byte[checkedLength(input.readInt(), bytes.length)];
        input.readFully(headerBytes);
        Properties header = new Properties();
        header.load(new InputStreamReader(new ByteArrayInputStream(headerBytes), StandardCharsets.UTF_8));
        byte[] metadata = new byte[checkedLength(input.readInt(), bytes.length / ChunkIndex.ID_LENGTH)
                * ChunkIndex.ID_LENGTH];
        input.readFully(metadata);
        if (input.read() != -1) throw new IOException("Trailing data in archive");
        if (!VERSION.equals(header.getProperty("version"))) throw new IOException("Unsupported archive version");
        try {
            if (Long.parseLong(header.getProperty("createdAt", "")) < 0) throw new NumberFormatException();
            Integer.parseInt(header.getProperty("files", "0"));
            Long.parseLong(header.getProperty("size", "0"));
        } catch (NumberFormatException e) {
            throw new IOException("Invalid archive header");
        }
        String name = file.getName();
        if (name.endsWith(ARCHIVE_EXTENSION)) name = name.substring(0, name.length() - ARCHIVE_EXTENSION.length());
        return new ArchiveInfo(name, file, header, metadata);
    }

    private static int checkedLength(int length, int limit) throws IOException {
        if (length < 0 || length > limit) throw new IOException("Invalid archive length field");
        return length;
    }

    static byte[] encodeArchive(Properties header, byte[] metadataChunks) throws IOException {
        ByteArrayOutputStream headerBytes = new ByteArrayOutputStream();
        header.store(new java.io.OutputStreamWriter(headerBytes, StandardCharsets.UTF_8), null);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(buffer);
        output.write(ARCHIVE_MAGIC);
        output.writeInt(headerBytes.size());
        headerBytes.writeTo(output);
        output.writeInt(metadataChunks.length / ChunkIndex.ID_LENGTH);
        output.write(metadataChunks);
        output.flush();
        buffer.write(sha256().digest(buffer.toByteArray()));
        return buffer.toByteArray();
    }

    public interface ItemVisitor {

        void visit(Item item) throws IOException;
    }

    /** Visits every file of the archive, sorted by path. */
    public void readItems(ArchiveInfo archive, ItemVisitor visitor) throws IOException {
        try (DataInputStream input = new DataInputStream(
                new BufferedInputStream(openChunks(archive.metadataChunks), 64 * 1024))) {
            if (input.readInt() != ITEMS_MAGIC || input.readInt() != 1) throw new IOException("Bad archive item list");
            while (true) {
                byte type = input.readByte();
                if (type == 0) break;
                if (type != 1) throw new IOException("Bad archive item type " + type);
                String path = input.readUTF();
                long size = input.readLong();
                long modified = input.readLong();
                int crc = input.readInt();
                int count = input.readInt();
                if (count < 0 || count > Integer.MAX_VALUE / ChunkIndex.ID_LENGTH) {
                    throw new IOException("Bad chunk count for " + path);
                }
                byte[] chunks = new byte[count * ChunkIndex.ID_LENGTH];
                input.readFully(chunks);
                visitor.visit(new Item(path, size, modified, crc, chunks));
            }
            if (input.read() != -1) throw new IOException("Trailing data in archive item list");
        }
    }

    public RepositoryWriter newWriter(int compressionLevel) throws IOException {
        requireWritable();
        return new RepositoryWriter(this, compressionLevel);
    }

    public void deleteArchive(String name) throws IOException {
        requireWritable();
        Files.delete(archiveFile(name).toPath());
    }

    // ---- Usage and compaction ----

    /**
     * Tracks which chunks a set of archives references, to tell how much space deleting some of them would free. Chunks
     * no archive references remain on disk until {@link #compact}.
     */
    public final class Usage {

        private final int[] references = new int[index.capacity()];
        private long liveBytes;

        public void add(ArchiveInfo archive) throws IOException {
            update(archive, 1);
            liveBytes += archive.file.length();
        }

        public void remove(ArchiveInfo archive) throws IOException {
            update(archive, -1);
            liveBytes -= archive.file.length();
        }

        /** Bytes in packs and archive files that the added archives need. */
        public long liveBytes() {
            return liveBytes;
        }

        private void update(ArchiveInfo archive, int delta) throws IOException {
            count(archive.metadataChunks, delta);
            readItems(archive, item -> count(item.chunks, delta));
        }

        private void count(byte[] chunks, int delta) {
            for (int offset = 0; offset < chunks.length; offset += ChunkIndex.ID_LENGTH) {
                int slot = index.find(chunks, offset);
                if (slot < 0) continue;
                int before = references[slot];
                references[slot] += delta;
                if (before == 0 && delta > 0) liveBytes += index.recordLength(slot);
                else if (references[slot] == 0 && before > 0) liveBytes -= index.recordLength(slot);
            }
        }
    }

    public Usage usage() {
        return new Usage();
    }

    public static final class CompactResult {

        public int deletedPacks;
        public int rewrittenPacks;
        public long freedBytes;
    }

    /**
     * Deletes chunks no archive references. Packs without live chunks are deleted; packs with more than
     * {@code threshold} of their bytes unreferenced are rewritten, like {@code borg compact}.
     */
    public CompactResult compact(double threshold) throws IOException {
        requireWritable();
        BitSet live = new BitSet(index.capacity());
        for (String name : archiveNames()) {
            ArchiveInfo archive;
            try {
                archive = readInfo(name);
                mark(live, archive.metadataChunks);
                readItems(archive, item -> mark(live, item.chunks));
            } catch (IOException e) {
                if (e instanceof InterruptedIOException) throw e;
                // Its chunks cannot be told apart from garbage, so deleting anything could destroy them.
                throw new IOException("Not compacting: archive " + name + " is unreadable: " + e.getMessage(), e);
            }
        }
        Map<Integer, long[]> usage = new TreeMap<>();
        for (Map.Entry<Integer, Long> pack : packLengths.entrySet()) {
            usage.put(pack.getKey(), new long[] { pack.getValue() - PackFile.MAGIC.length, 0 });
        }
        for (int slot = 0; slot < index.capacity(); slot++) {
            if (index.isUsed(slot) && live.get(slot)) usage.get(index.pack(slot))[1] += index.recordLength(slot);
        }
        Set<Integer> dead = new HashSet<>();
        Set<Integer> sparse = new HashSet<>();
        for (Map.Entry<Integer, long[]> pack : usage.entrySet()) {
            long total = pack.getValue()[0], used = pack.getValue()[1];
            if (used == 0) dead.add(pack.getKey());
            else if (total - used > total * threshold) sparse.add(pack.getKey());
        }
        CompactResult result = new CompactResult();
        if (dead.isEmpty() && sparse.isEmpty()) return result;

        // Copy live records of sparse packs, in pack order, into new packs.
        Map<Integer, List<Integer>> moving = new HashMap<>();
        for (int slot = 0; slot < index.capacity(); slot++) {
            if (index.isUsed(slot) && live.get(slot) && sparse.contains(index.pack(slot))) {
                moving.computeIfAbsent(index.pack(slot), pack -> new ArrayList<>()).add(slot);
            }
        }
        ChunkIndex compacted = new ChunkIndex(index.size());
        Map<Integer, Long> newLengths = new TreeMap<>(packLengths);
        List<PackFile.Writer> written = new ArrayList<>();
        PackFile.Writer writer = null;
        try {
            for (Map.Entry<Integer, List<Integer>> pack : new TreeMap<>(moving).entrySet()) {
                List<Integer> slots = pack.getValue();
                slots.sort((a, b) -> Integer.compare(index.offset(a), index.offset(b)));
                RandomAccessFile source = reader(pack.getKey());
                for (int slot : slots) {
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Cancelled");
                    PackFile.Record record = PackFile.read(source, index.offset(slot), index.storedLength(slot));
                    if (writer == null || writer.position() >= PackFile.TARGET_SIZE) {
                        if (writer != null) writer.close();
                        int id = allocatePack();
                        writer = new PackFile.Writer(new File(data, PackFile.name(id)), id);
                        written.add(writer);
                    }
                    int offset = writer.append(
                            record.id,
                            record.method,
                            record.rawLength,
                            record.stored,
                            record.storedLength,
                            record.crc);
                    compacted.put(record.id, 0, writer.pack, offset, record.storedLength, record.rawLength);
                }
            }
            if (writer != null) writer.close();
        } catch (IOException | RuntimeException e) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException suppressed) {
                    e.addSuppressed(suppressed);
                }
            }
            // The originals are untouched; the copies are unindexed and would only be rescanned as duplicates.
            for (PackFile.Writer copy : written) copy.file.delete();
            throw e;
        }
        for (int slot = 0; slot < index.capacity(); slot++) {
            if (!index.isUsed(slot)) continue;
            int pack = index.pack(slot);
            if (dead.contains(pack) || sparse.contains(pack)) continue;
            byte[] id = index.id(slot);
            compacted.put(id, 0, pack, index.offset(slot), index.storedLength(slot), index.rawLength(slot));
        }
        for (PackFile.Writer copy : written) newLengths.put(copy.pack, copy.file.length());
        long before = 0, after = 0;
        for (long length : packLengths.values()) before += length;
        for (int pack : dead) newLengths.remove(pack);
        for (int pack : sparse) newLengths.remove(pack);
        for (long length : newLengths.values()) after += length;
        index = compacted;
        packLengths.clear();
        packLengths.putAll(newLengths);
        // Persist the new locations before deleting anything they replace.
        saveIndex();
        Set<Integer> obsolete = new HashSet<>(dead);
        obsolete.addAll(sparse);
        for (int pack : obsolete) {
            closeReader(pack);
            File file = new File(data, PackFile.name(pack));
            try {
                Files.delete(file.toPath());
            } catch (IOException e) {
                // Left behind, it is rescanned on the next open; its chunks are only duplicates then.
                warn("Could not delete obsolete pack " + file.getName() + ": " + e.getMessage());
            }
        }
        result.deletedPacks = dead.size();
        result.rewrittenPacks = sparse.size();
        result.freedBytes = before - after;
        return result;
    }

    private void mark(BitSet live, byte[] chunks) {
        for (int offset = 0; offset < chunks.length; offset += ChunkIndex.ID_LENGTH) {
            int slot = index.find(chunks, offset);
            if (slot >= 0) live.set(slot);
        }
    }

    // ---- Checking ----

    public interface Progress {

        void report(String message);
    }

    public static final class CheckResult {

        public int archives;
        public int brokenArchives;
        public long missingChunks;
        public long corruptChunks;
        public long checkedChunks;

        public boolean ok() {
            return brokenArchives == 0 && missingChunks == 0 && corruptChunks == 0;
        }
    }

    /**
     * Checks that every archive is readable and every chunk it references exists. With {@code verifyData}, every chunk
     * in every pack is also read, decompressed and hashed.
     */
    public CheckResult check(boolean verifyData, Progress progress) throws IOException {
        CheckResult result = new CheckResult();
        if (verifyData) {
            Set<Integer> packs = new HashSet<>(packLengths.keySet());
            for (int pack : packs) {
                progress.report("Verifying pack " + PackFile.name(pack));
                File file = new File(data, PackFile.name(pack));
                long intact = PackFile.scan(file, (record, offset) -> {
                    result.checkedChunks++;
                    try {
                        byte[] raw = decode(record);
                        if (!Arrays.equals(sha256.digest(raw), record.id)) throw new IOException("hash mismatch");
                    } catch (IOException e) {
                        result.corruptChunks++;
                        progress.report("  corrupt chunk " + hex(record.id) + " at offset " + offset);
                    }
                });
                if (intact < file.length()) {
                    result.corruptChunks++;
                    progress.report("  " + (file.length() - intact) + " unreadable bytes at offset " + intact);
                }
            }
        }
        for (String name : archiveNames()) {
            result.archives++;
            long[] missing = { 0 };
            try {
                ArchiveInfo archive = readInfo(name);
                missing[0] += countMissing(archive.metadataChunks);
                readItems(archive, item -> missing[0] += countMissing(item.chunks));
            } catch (IOException e) {
                if (e instanceof InterruptedIOException) throw e;
                result.brokenArchives++;
                progress.report("Archive " + name + " is unreadable: " + e.getMessage());
                continue;
            }
            if (missing[0] > 0) {
                result.brokenArchives++;
                result.missingChunks += missing[0];
                progress.report("Archive " + name + " references " + missing[0] + " missing chunks");
            }
        }
        return result;
    }

    private long countMissing(byte[] chunks) {
        long missing = 0;
        for (int offset = 0; offset < chunks.length; offset += ChunkIndex.ID_LENGTH) {
            if (index.find(chunks, offset) < 0) missing++;
        }
        return missing;
    }

    // ---- Utilities ----

    static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
            builder.append(Character.forDigit((b >> 4) & 15, 16)).append(Character.forDigit(b & 15, 16));
        return builder.toString();
    }

    /** Writes a file through a synced temporary, so a crash leaves either the old or the new contents. */
    static void writeAtomic(File file, byte[] bytes) throws IOException {
        Path target = file.toPath();
        Path temporary = Files.createTempFile(target.getParent(), ".su-save-", ".tmp");
        try {
            try (FileOutputStream output = new FileOutputStream(temporary.toFile())) {
                output.write(bytes);
                output.getFD().sync();
            }
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    static void writeSynced(File file, byte[] bytes) throws IOException {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(bytes);
            output.getFD().sync();
        }
    }

    @Override
    public void close() throws IOException {
        for (RandomAccessFile reader : readers.values()) {
            try {
                reader.close();
            } catch (IOException ignored) {}
        }
        readers.clear();
        inflater.end();
        try {
            if (lock.isValid()) lock.release();
        } catch (IOException ignored) {
            // Closing the file below releases it as well.
        } finally {
            lockFile.close();
        }
    }

}
