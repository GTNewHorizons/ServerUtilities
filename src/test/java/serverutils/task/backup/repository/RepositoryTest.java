package serverutils.task.backup.repository;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import serverutils.lib.util.FileUtils;

public class RepositoryTest {

    private Path root;
    private File repositoryFolder;

    @Before
    public void createFolders() throws IOException {
        Files.createDirectories(new File("build").toPath());
        root = Files.createTempDirectory(new File("build").toPath(), "repository-test-");
        repositoryFolder = root.resolve("repo").toFile();
    }

    @After
    public void deleteFolders() {
        FileUtils.delete(root.toFile());
    }

    private static byte[] random(int size, long seed) {
        byte[] bytes = new byte[size];
        new Random(seed).nextBytes(bytes);
        return bytes;
    }

    private static List<byte[]> chunks(byte[] data, Chunker.Params params) throws IOException {
        List<byte[]> chunks = new ArrayList<>();
        Chunker chunker = new Chunker(params, (buffer, length) -> chunks.add(java.util.Arrays.copyOf(buffer, length)));
        // Odd feed sizes must not change the boundaries.
        for (int offset = 0; offset < data.length; offset += 7777) {
            chunker.update(data, offset, Math.min(7777, data.length - offset));
        }
        chunker.finish();
        return chunks;
    }

    private static Set<String> hashes(List<byte[]> chunks) {
        Set<String> hashes = new HashSet<>();
        for (byte[] chunk : chunks) hashes.add(Repository.hex(Repository.sha256().digest(chunk)));
        return hashes;
    }

    @Test
    public void chunkBoundariesResynchronizeAfterAnInsertion() throws IOException {
        byte[] original = random(4 * 1024 * 1024, 1);
        byte[] edited = new byte[original.length + 100];
        System.arraycopy(original, 0, edited, 0, 2_000_000);
        System.arraycopy(random(100, 2), 0, edited, 2_000_000, 100);
        System.arraycopy(original, 2_000_000, edited, 2_000_100, original.length - 2_000_000);
        List<byte[]> before = chunks(original, Chunker.Params.DATA);
        List<byte[]> after = chunks(edited, Chunker.Params.DATA);
        int total = 0;
        for (byte[] chunk : before) {
            assertTrue(chunk.length <= Chunker.Params.DATA.maxSize);
            total += chunk.length;
        }
        assertEquals(original.length, total);
        assertTrue("Expected several chunks, got " + before.size(), before.size() > 20);
        Set<String> shared = hashes(before);
        shared.retainAll(hashes(after));
        assertTrue(
                "Only the chunks around the insertion should differ: " + shared.size() + " of " + before.size(),
                shared.size() >= before.size() - 3);
    }

    private ArchiveInfo backup(Map<String, byte[]> files, String name, long created) throws IOException {
        try (Repository repository = Repository.openOrCreate(repositoryFolder, 0);
                RepositoryWriter writer = repository.newWriter(1)) {
            for (Map.Entry<String, byte[]> file : files.entrySet()) {
                CRC32 crc = new CRC32();
                crc.update(file.getValue());
                writer.addStream(
                        file.getKey(),
                        created,
                        new ByteArrayInputStream(file.getValue()),
                        file.getValue().length,
                        (int) crc.getValue());
            }
            Properties header = new Properties();
            header.setProperty("createdAt", Long.toString(created));
            header.setProperty("customName", "false");
            header.setProperty("worldName", "World");
            File output = repository.archiveFile(name);
            writer.commit(header, output);
            return repository.readInfo(name);
        }
    }

    private Map<String, byte[]> read(String name) throws IOException {
        Map<String, byte[]> files = new HashMap<>();
        try (Repository repository = Repository.open(repositoryFolder, false, 0)) {
            ArchiveInfo info = repository.readInfo(name);
            repository.readItems(info, item -> {
                try (InputStream input = repository.openItem(item)) {
                    byte[] bytes = IOUtils.toByteArray(input);
                    assertEquals(item.size, bytes.length);
                    CRC32 crc = new CRC32();
                    crc.update(bytes);
                    assertEquals(item.crc, (int) crc.getValue());
                    files.put(item.path, bytes);
                }
            });
        }
        return files;
    }

    private static Map<String, byte[]> world(long seed) {
        Map<String, byte[]> files = new HashMap<>();
        files.put("World/level.dat", random(900, seed));
        files.put("World/data/big.bin", random(3 * 1024 * 1024, 10));
        files.put("World/data/empty.dat", new byte[0]);
        files.put("World/data/zeros.dat", new byte[600_000]);
        files.put("serverutilities/server/ranks.txt", "ranks".getBytes(StandardCharsets.UTF_8));
        return files;
    }

    private static void assertSameFiles(Map<String, byte[]> expected, Map<String, byte[]> actual) {
        assertEquals(expected.keySet(), actual.keySet());
        for (Map.Entry<String, byte[]> file : expected.entrySet()) {
            assertArrayEquals(file.getKey(), file.getValue(), actual.get(file.getKey()));
        }
    }

    @Test
    public void archivesRoundTripAndDeduplicateUnchangedData() throws IOException {
        Map<String, byte[]> first = world(1);
        ArchiveInfo info = backup(first, "first", 1000);
        assertEquals(first.size(), info.fileCount());
        assertEquals("World/", info.worldPath());
        assertEquals(1000, info.created());
        long afterFirst = diskSize();
        assertTrue("Zeros must compress", afterFirst < 3 * 1024 * 1024 + 300_000);

        Map<String, byte[]> second = world(2);
        byte[] big = second.get("World/data/big.bin").clone();
        System.arraycopy(random(10_000, 3), 0, big, 1_500_000, 10_000);
        second.put("World/data/big.bin", big);
        second.put("World/data/new.dat", random(5000, 4));
        backup(second, "second", 2000);
        long added = diskSize() - afterFirst;
        assertTrue("Second backup should only store the changes, added " + added, added < 200_000);

        assertSameFiles(first, read("first"));
        assertSameFiles(second, read("second"));
        try (Repository repository = Repository.open(repositoryFolder, false, 0)) {
            assertEquals(java.util.Arrays.asList("first", "second"), repository.archiveNames());
            assertTrue(repository.check(true, message -> {}).ok());
        }
    }

    private long diskSize() throws IOException {
        try (Repository repository = Repository.open(repositoryFolder, false, 0)) {
            return repository.diskSize();
        }
    }

    @Test
    public void regionFilesAreCutAtMinecraftChunkBoundaries() throws IOException {
        // Location table: 64 chunks of 3 sectors each, starting after the header.
        byte[] region = new byte[RegionChunker.HEADER + 64 * 3 * RegionChunker.SECTOR];
        new Random(5).nextBytes(region);
        for (int i = 0; i < 1024; i++) {
            int entry = i < 64 ? (2 + i * 3) << 8 | 3 : 0;
            region[i * 4] = (byte) (entry >>> 24);
            region[i * 4 + 1] = (byte) (entry >>> 16);
            region[i * 4 + 2] = (byte) (entry >>> 8);
            region[i * 4 + 3] = (byte) entry;
        }
        List<Integer> cuts = new ArrayList<>();
        int[] position = { 0 };
        RegionChunker chunker = new RegionChunker(32 * 1024, 512 * 1024, (data, length) -> {
            position[0] += length;
            cuts.add(position[0]);
        });
        for (int offset = 0; offset < region.length; offset += 5000) {
            chunker.update(region, offset, Math.min(5000, region.length - offset));
        }
        chunker.finish();
        assertEquals(RegionChunker.SECTOR, (int) cuts.get(0));
        assertEquals(RegionChunker.HEADER, (int) cuts.get(1));
        assertEquals(region.length, (int) cuts.get(cuts.size() - 1));
        for (int cut : cuts.subList(2, cuts.size() - 1)) {
            assertEquals("Cut inside a Minecraft chunk", 0, (cut - RegionChunker.HEADER) % (3 * RegionChunker.SECTOR));
        }

        Map<String, byte[]> files = new HashMap<>();
        files.put("World/region/r.1.1.mca", region);
        backup(files, "region-1", 1000);
        long before = diskSize();
        // Minecraft rewrites one chunk in place and updates its timestamp.
        byte[] changed = region.clone();
        System.arraycopy(random(3 * RegionChunker.SECTOR, 6), 0, changed, RegionChunker.HEADER + 30 * 12288, 12288);
        changed[RegionChunker.SECTOR + 30 * 4] ^= 1;
        files.put("World/region/r.1.1.mca", changed);
        backup(files, "region-2", 2000);
        long added = diskSize() - before;
        // The timestamp table plus one 36 KiB group of whole chunks, and the archive's small metadata.
        assertTrue("Changing one chunk added " + added + " bytes", added < 60_000);
        assertSameFiles(files, read("region-2"));
    }

    @Test
    public void filesCacheSkipsUnchangedFilesButNotModifiedOnes() throws IOException {
        Path world = Files.createDirectories(root.resolve("world"));
        Path stable = world.resolve("stable.dat");
        Path changing = world.resolve("changing.dat");
        Files.write(stable, random(100_000, 1));
        Files.write(changing, random(100_000, 2));
        // Recent timestamps are never trusted, as a write in the same tick could leave them unchanged.
        FileTime old = FileTime.fromMillis(System.currentTimeMillis() - 60_000);
        Files.setLastModifiedTime(stable, old);
        Files.setLastModifiedTime(changing, old);
        int[] unchanged = new int[3];
        for (int run = 0; run < 3; run++) {
            if (run == 2) {
                Files.write(changing, random(100_000, 3));
                Files.setLastModifiedTime(changing, FileTime.fromMillis(old.toMillis() + 1000));
            }
            try (Repository repository = Repository.openOrCreate(repositoryFolder, 0);
                    RepositoryWriter writer = repository.newWriter(1)) {
                writer.addFile("world/stable.dat", stable.toFile());
                writer.addFile("world/changing.dat", changing.toFile());
                Properties header = new Properties();
                header.setProperty("createdAt", Long.toString(run));
                header.setProperty("customName", "false");
                writer.commit(header, repository.archiveFile("run-" + run));
                unchanged[run] = writer.unchangedFiles;
            }
        }
        assertArrayEquals(new int[] { 0, 2, 1 }, unchanged);
        Map<String, byte[]> restored = read("run-2");
        assertArrayEquals(Files.readAllBytes(changing), restored.get("world/changing.dat"));
        assertArrayEquals(Files.readAllBytes(stable), restored.get("world/stable.dat"));

        // A cache entry is never used once compaction removed its chunks.
        try (Repository repository = Repository.open(repositoryFolder, true, 0)) {
            for (String name : repository.archiveNames()) repository.deleteArchive(name);
            repository.compact(0.1);
            assertEquals(0, repository.chunkCount());
        }
        try (Repository repository = Repository.open(repositoryFolder, true, 0);
                RepositoryWriter writer = repository.newWriter(1)) {
            writer.addFile("world/stable.dat", stable.toFile());
            assertEquals(0, writer.unchangedFiles);
            Properties header = new Properties();
            header.setProperty("createdAt", "5");
            writer.commit(header, repository.archiveFile("after-compact"));
        }
        assertArrayEquals(Files.readAllBytes(stable), read("after-compact").get("world/stable.dat"));
    }

    @Test
    public void indexIsRebuiltFromPacksWhenMissingCorruptOrStale() throws IOException {
        Map<String, byte[]> files = world(1);
        backup(files, "a", 1);
        File index = new File(repositoryFolder, "index");
        Files.delete(index.toPath());
        assertSameFiles(files, read("a"));
        try (Repository repository = Repository.open(repositoryFolder, true, 0)) {
            assertTrue(repository.warnings().isEmpty());
        }
        try (RandomAccessFile file = new RandomAccessFile(index, "rw")) {
            file.seek(40);
            file.write(file.read() ^ 0xff);
        }
        try (Repository repository = Repository.open(repositoryFolder, true, 0)) {
            assertTrue(repository.warnings().get(0), repository.warnings().get(0).contains("unreadable"));
        }
        assertSameFiles(files, read("a"));
    }

    @Test
    public void interruptedBackupsLeaveReusableChunksAndTornPacksStayReadable() throws IOException {
        Map<String, byte[]> files = world(1);
        try (Repository repository = Repository.openOrCreate(repositoryFolder, 0);
                RepositoryWriter writer = repository.newWriter(1)) {
            byte[] big = files.get("World/data/big.bin");
            CRC32 crc = new CRC32();
            crc.update(big);
            writer.addStream("World/data/big.bin", 1, new ByteArrayInputStream(big), big.length, (int) crc.getValue());
            // No commit: the backup was cancelled.
        }
        File[] packs = new File(repositoryFolder, "data").listFiles();
        assertEquals(1, packs.length);
        // A crash while appending leaves a partial record behind.
        Files.write(
                packs[0].toPath(),
                new byte[] { 0x53, 0x55, 0x43, 0x31, 9, 9 },
                java.nio.file.StandardOpenOption.APPEND);
        Files.delete(new File(repositoryFolder, "index").toPath());
        try (Repository repository = Repository.openOrCreate(repositoryFolder, 0);
                RepositoryWriter writer = repository.newWriter(1)) {
            assertTrue(repository.warnings().toString(), repository.warnings().toString().contains("damaged trailing"));
            byte[] big = files.get("World/data/big.bin");
            CRC32 crc = new CRC32();
            crc.update(big);
            writer.addStream("World/data/big.bin", 1, new ByteArrayInputStream(big), big.length, (int) crc.getValue());
            assertEquals("Chunks of the cancelled backup are reused", 0, writer.newChunks);
            Properties header = new Properties();
            header.setProperty("createdAt", "1");
            writer.commit(header, repository.archiveFile("after-crash"));
        }
        assertArrayEquals(files.get("World/data/big.bin"), read("after-crash").get("World/data/big.bin"));
    }

    @Test
    public void compactionFreesOnlyUnreferencedChunks() throws IOException {
        Map<String, byte[]> first = world(1);
        backup(first, "first", 1);
        Map<String, byte[]> second = new HashMap<>(first);
        second.put("World/data/big.bin", random(3 * 1024 * 1024, 99));
        backup(second, "second", 2);
        long before = diskSize();
        try (Repository repository = Repository.open(repositoryFolder, true, 0)) {
            Repository.Usage usage = repository.usage();
            usage.add(repository.readInfo("first"));
            usage.add(repository.readInfo("second"));
            long both = usage.liveBytes();
            usage.remove(repository.readInfo("first"));
            long freed = both - usage.liveBytes();
            assertTrue("Only the first region is unique to the first archive: " + freed, freed > 3_000_000);
            assertTrue(freed < 3_300_000);

            repository.deleteArchive("first");
            Repository.CompactResult result = repository.compact(0.1);
            assertTrue(result.freedBytes > 3_000_000);
            assertTrue(repository.check(true, message -> {}).ok());
        }
        assertTrue(diskSize() < before - 3_000_000);
        assertSameFiles(second, read("second"));
        // Index and packs agree after reopening.
        try (Repository repository = Repository.open(repositoryFolder, false, 0)) {
            assertTrue(repository.warnings().toString(), repository.warnings().isEmpty());
        }
    }

    @Test
    public void compactionRefusesToRunWithAnUnreadableArchive() throws IOException {
        backup(world(1), "first", 1);
        backup(world(2), "second", 2);
        Files.write(new File(repositoryFolder, "archives/broken.archive").toPath(), new byte[] { 1, 2, 3 });
        try (Repository repository = Repository.open(repositoryFolder, true, 0)) {
            repository.deleteArchive("first");
            IOException error = assertThrows(IOException.class, () -> repository.compact(0.1));
            assertTrue(error.getMessage().contains("broken"));
        }
        Files.delete(new File(repositoryFolder, "archives/broken.archive").toPath());
        assertSameFiles(world(2), read("second"));
    }

    @Test
    public void checkFindsCorruptAndMissingChunks() throws IOException {
        backup(world(1), "a", 1);
        File pack = new File(repositoryFolder, "data").listFiles()[0];
        try (RandomAccessFile file = new RandomAccessFile(pack, "rw")) {
            file.seek(PackFile.MAGIC.length + PackFile.RECORD_HEADER + 10);
            int b = file.read();
            file.seek(PackFile.MAGIC.length + PackFile.RECORD_HEADER + 10);
            file.write(b ^ 0x40);
        }
        try (Repository repository = Repository.open(repositoryFolder, false, 0)) {
            List<String> messages = new ArrayList<>();
            Repository.CheckResult result = repository.check(true, messages::add);
            assertFalse(result.ok());
            assertTrue(result.corruptChunks > 0);
        }
        Files.delete(pack.toPath());
        Files.delete(new File(repositoryFolder, "index").toPath());
        try (Repository repository = Repository.open(repositoryFolder, false, 0)) {
            Repository.CheckResult result = repository.check(false, message -> {});
            assertEquals(1, result.brokenArchives);
        }
    }

    @Test
    public void writersExcludeEachOtherAndReaders() throws IOException {
        Repository.create(repositoryFolder);
        try (Repository writer = Repository.open(repositoryFolder, true, 0)) {
            IOException error = assertThrows(IOException.class, () -> Repository.open(repositoryFolder, false, 300));
            assertTrue(error.getMessage().contains("in use"));
        }
        try (Repository reader = Repository.open(repositoryFolder, false, 0)) {
            assertNotNull(reader);
            assertThrows(IOException.class, () -> reader.newWriter(1));
        }
    }

    // ---- Tool ----

    private int tool(ByteArrayOutputStream output, String... args) {
        PrintStream out = new PrintStream(output, true);
        String[] full = new String[args.length + 2];
        full[0] = "--repo";
        full[1] = repositoryFolder.getPath();
        System.arraycopy(args, 0, full, 2, args.length);
        return new RepositoryTool(out, out).run(full);
    }

    @Test
    public void toolRestoresExportsAndProtectsExistingFiles() throws IOException {
        Map<String, byte[]> files = world(1);
        backup(files, "2026-01-01-00-00-00", 1_700_000_000_000L);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertEquals(output.toString(), 0, tool(output, "list"));
        assertTrue(output.toString().contains("2026-01-01-00-00-00"));
        assertEquals(output.toString(), 0, tool(output, "info", "latest"));
        output.reset();
        assertEquals(output.toString(), 0, tool(output, "files", "latest", "World/**"));
        assertTrue(output.toString().contains("World/data/big.bin"));
        assertFalse(output.toString().contains("ranks.txt"));

        Path target = root.resolve("server");
        assertEquals(output.toString(), 0, tool(output, "restore", "latest", target.toString()));
        for (Map.Entry<String, byte[]> file : files.entrySet()) {
            Path restored = target.resolve(file.getKey());
            assertArrayEquals(file.getValue(), Files.readAllBytes(restored));
            assertEquals(1_700_000_000_000L, Files.getLastModifiedTime(restored).toMillis());
        }

        Files.write(target.resolve("World/level.dat"), new byte[] { 7 });
        output.reset();
        assertEquals(
                "Existing files are not replaced by default",
                1,
                tool(output, "restore", "latest", target.toString()));
        assertTrue(output.toString(), output.toString().contains("World/level.dat"));
        assertArrayEquals(new byte[] { 7 }, Files.readAllBytes(target.resolve("World/level.dat")));

        assertEquals(
                output.toString(),
                0,
                tool(output, "restore", "latest", target.toString(), "--world-only", "--overwrite"));
        assertArrayEquals(files.get("World/level.dat"), Files.readAllBytes(target.resolve("World/level.dat")));
        Path displaced;
        try (java.util.stream.Stream<Path> paths = Files.list(target.resolve("backups_before_restore"))) {
            displaced = paths.findFirst().get();
        }
        assertArrayEquals(new byte[] { 7 }, Files.readAllBytes(displaced.resolve("World/level.dat")));

        Path partial = root.resolve("partial");
        assertEquals(
                output.toString(),
                0,
                tool(output, "restore", "latest", partial.toString(), "--include", "World/data/*"));
        assertTrue(Files.exists(partial.resolve("World/data/big.bin")));
        assertFalse(Files.exists(partial.resolve("World/level.dat")));

        File zip = root.resolve("export.zip").toFile();
        assertEquals(output.toString(), 0, tool(output, "export", "latest", zip.getPath()));
        try (ZipFile archive = new ZipFile(zip)) {
            assertEquals("World", archive.getComment());
            for (Map.Entry<String, byte[]> file : files.entrySet()) {
                ZipEntry entry = archive.getEntry(file.getKey());
                assertNotNull(file.getKey(), entry);
                assertArrayEquals(file.getValue(), IOUtils.toByteArray(archive.getInputStream(entry)));
            }
            assertNull("No world id, so no metadata entry", archive.getEntry(RepositoryTool.ZIP_METADATA_ENTRY));
        }
        assertEquals(1, tool(output, "export", "latest", zip.getPath()));

        assertEquals(output.toString(), 0, tool(output, "check", "--verify-data"));
        assertEquals(output.toString(), 0, tool(output, "delete", "2026-01-01-00-00-00"));
        assertEquals(2, tool(output, "bogus"));
        assertEquals(1, tool(output, "info", "latest"));
    }

    @Test
    public void replacingTheWorldMovesItAsideAndFailuresPutEverythingBack() throws IOException {
        Map<String, byte[]> files = world(1);
        backup(files, "a", 1);
        Path target = root.resolve("server");
        Files.createDirectories(target.resolve("World"));
        Files.createDirectories(target.resolve("serverutilities/server"));
        Files.write(target.resolve("World/level.dat"), new byte[] { 1 });
        Files.write(target.resolve("World/stale.dat"), new byte[] { 2 });
        Files.write(target.resolve("serverutilities/server/ranks.txt"), new byte[] { 3 });

        // Damage the last file restored, so the failure comes after everything else was written.
        try (Repository repository = Repository.open(repositoryFolder, false, 0)) {
            Item[] ranks = new Item[1];
            repository.readItems(
                    repository.readInfo("a"),
                    item -> { if (item.path.endsWith("ranks.txt")) ranks[0] = item; });
            int slot = repository.index.find(ranks[0].chunks, 0);
            File pack = new File(repositoryFolder, "data/" + PackFile.name(repository.index.pack(slot)));
            try (RandomAccessFile file = new RandomAccessFile(pack, "rw")) {
                long offset = repository.index.offset(slot) + PackFile.RECORD_HEADER;
                file.seek(offset);
                int b = file.read();
                file.seek(offset);
                file.write(b ^ 1);
            }
        }
        RepositoryRestore restore = new RepositoryRestore();
        restore.replaceWorld = true;
        restore.overwrite = true;
        try (Repository repository = Repository.open(repositoryFolder, false, 0)) {
            assertThrows(IOException.class, () -> restore.restore(repository, repository.readInfo("a"), target));
        }
        assertArrayEquals(new byte[] { 1 }, Files.readAllBytes(target.resolve("World/level.dat")));
        assertArrayEquals(new byte[] { 2 }, Files.readAllBytes(target.resolve("World/stale.dat")));
        assertArrayEquals(new byte[] { 3 }, Files.readAllBytes(target.resolve("serverutilities/server/ranks.txt")));
        assertFalse(Files.exists(target.resolve("World/data/big.bin")));
        assertFalse(Files.exists(target.resolve("World_old")));

        // The same restore succeeds once the damaged file is left out.
        restore.includes.add(java.nio.file.FileSystems.getDefault().getPathMatcher("glob:World/**"));
        RepositoryRestore.Result result;
        try (Repository repository = Repository.open(repositoryFolder, false, 0)) {
            result = restore.restore(repository, repository.readInfo("a"), target);
        }
        assertEquals(target.resolve("World_old").toAbsolutePath(), result.previousWorld);
        assertNull("The world folder was moved aside, so nothing was replaced", result.replacedFiles);
        assertArrayEquals(files.get("World/level.dat"), Files.readAllBytes(target.resolve("World/level.dat")));
        assertFalse("No files of the previous world are left behind", Files.exists(target.resolve("World/stale.dat")));
        assertArrayEquals(new byte[] { 2 }, Files.readAllBytes(target.resolve("World_old/stale.dat")));
    }

    @Test
    public void restoreRejectsPathsLeavingTheTarget() {
        Path target = new File("build/target").toPath().toAbsolutePath();
        for (String path : new String[] { "../x", "a/../../x", "/etc/passwd", "C:/x", "a\\b", "", "a//b", "a/./b" }) {
            assertThrows(path, IOException.class, () -> RepositoryRestore.restoreTarget(target, path));
        }
    }
}
