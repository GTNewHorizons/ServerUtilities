package serverutils.task.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import serverutils.ServerUtilitiesConfig;
import serverutils.lib.util.compression.CommonsCompressor;
import serverutils.lib.util.compression.ICompress;
import serverutils.lib.util.compression.LegacyCompressor;

public class ArchiveStoreTest extends BackupFixtures.Tests {

    private static final String WORLD = "12345678-1234-1234-1234-123456789abc";
    private static final byte[] LEVEL = { 1 };

    @Before
    public void beforeEach() {
        testSetup(SetupFlags.FOLDERS);
    }

    @After
    public void afterEach() {
        testReset();
    }

    @Test
    public void list_SkipsDirectoriesAndNonArchives() throws IOException {
        File archive = new File(backups, "archive.zip");
        BackupFixtures.writeArchive(archive, UUID.randomUUID().toString(), 1_000);
        Files.createDirectory(new File(backups, "directory.zip").toPath());
        Files.write(new File(backups, "noise.txt").toPath(), new byte[1_000]);
        // a timestamp prefix does not make a file an archive
        Files.write(new File(backups, "2020-01-01-00-00-00-not-a-zip.dat").toPath(), new byte[1_000]);
        // an archive still being written is not one retention may delete
        Files.write(new File(backups, ".su-save-123.tmp").toPath(), new byte[1_000]);

        List<ArchiveEntry> entries = new ArchiveStore(backups).list();

        assertEquals(1, entries.size());
        assertEquals(archive, entries.get(0).file);
        assertNull(entries.get(0).problem());
    }

    @Test
    public void list_SkipsSymlinks() throws IOException {
        File target = new File(build, "target.zip");
        BackupFixtures.writeArchive(target, WORLD, 1_000);
        File directory = Files.createDirectory(new File(build, "linked-world").toPath()).toFile();
        try {
            Files.createSymbolicLink(new File(backups, "linked.zip").toPath(), target.getAbsoluteFile().toPath());
            Files.createSymbolicLink(
                    new File(backups, "linked-world.zip").toPath(),
                    directory.getAbsoluteFile().toPath());
        } catch (FileSystemException | UnsupportedOperationException ex) {
            Assume.assumeNoException("Symbolic links unavailable on this host", ex);
        }

        assertTrue("Links are never retention candidates", new ArchiveStore(backups).list().isEmpty());
    }

    @Test
    public void list_WhenArchiveUnreadable_KeepsItWithProblem() throws IOException {
        File unreadable = new File(backups, "unreadable.zip");
        Files.write(unreadable.toPath(), new byte[] { 1, 2, 3 });

        List<ArchiveEntry> entries = new ArchiveStore(backups).list();

        assertEquals(1, entries.size());
        assertEquals(unreadable, entries.get(0).file);
        assertNotNull(entries.get(0).problem());
    }

    @Test
    public void list_WhenInterrupted_Throws() throws IOException {
        BackupFixtures.writeArchive(new File(backups, "archive.zip"), WORLD, 1_000);

        Thread.currentThread().interrupt();
        try {
            assertThrows(InterruptedIOException.class, () -> new ArchiveStore(backups).list());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void read_RecognizesWorldFolderLayouts() throws IOException {
        String[][] layouts = { { null, "world/level.dat" }, { null, "worlds/world/level.dat" },
                { "world", "world/level.dat_old" }, { "world", "saves/world/level.dat_old" },
                { "world", "worlds/world/level.dat_old" } };
        for (String[] layout : layouts) {
            File file = zip("layout.zip", layout[0], Collections.singletonMap(layout[1], LEVEL));

            ArchiveEntry entry = entry(file);

            assertNull(layout[1], entry.problem());
            assertEquals(layout[1], "name:world", entry.world());
            Files.delete(file.toPath());
        }
    }

    @Test
    public void read_UnsafeOrAmbiguousWorldFolder_HasProblem() throws IOException {
        int index = 0;
        for (String prefix : new String[] { "../world/", "parent/../world/", "/world/", "C:/world/", "parent//world/",
                "parent/./world/" }) {
            zip("unsafe-" + index++ + ".zip", "world", Collections.singletonMap(prefix + "level.dat", LEVEL));
        }
        zip("unsafe-uncommented.zip", null, Collections.singletonMap("../world/level.dat", LEVEL));
        zip("ambiguous.zip", "world", levels("worlds/world/level.dat", "other/world/level.dat"));
        zip("ambiguous-uncommented.zip", null, levels("world/level.dat", "other/level.dat"));

        List<ArchiveEntry> entries = new ArchiveStore(backups).list();

        assertEquals(9, entries.size());
        for (ArchiveEntry entry : entries) {
            assertNotNull(entry.id(), entry.problem());
            assertThrows(entry.id(), IOException.class, () -> ArchiveStore.isCustomWorldArchive(entry.file));
        }
    }

    @Test
    public void read_NoUsableLevelDat_HasProblem() throws IOException {
        zip("metadata-only.zip", "world", metadataEntry(BackupFixtures.metadata(WORLD, 1_000, "false")));
        zip("comment-only.zip", "world", levels("unrelated.txt"));
        zip("uncommented.zip", null, levels("file"));
        zip("wrong-world.zip", "world", levels("saves/other/level.dat"));
        zip("directory-marker.zip", "world", Collections.singletonMap("saves/world/level.dat/", new byte[0]));
        zip("empty-marker.zip", "world", Collections.singletonMap("saves/world/level.dat", new byte[0]));
        zip("two-layouts.zip", "world", levels("world/level.dat", "saves/world/level.dat"));

        List<ArchiveEntry> entries = new ArchiveStore(backups).list();

        assertEquals(7, entries.size());
        for (ArchiveEntry entry : entries) {
            assertNotNull(entry.id(), entry.problem());
        }
    }

    @Test
    public void read_MetadataOverridesFilenameAndMtime() throws IOException {
        long created = 1_000_000;
        File timestampNamed = metadataZip("2026-10-05-00-00-00.zip", BackupFixtures.metadata(WORLD, created, "true"));
        File renamed = metadataZip("renamed-automatic.zip", BackupFixtures.metadata(WORLD, created, "false"));
        File legacyCustom = zip("pre-metadata-milestone.zip", "world", levels("world/level.dat"));
        File legacyAutomatic = zip("2026-10-05-12-00-00.zip", "world", levels("world/level.dat"));
        for (File file : Arrays.asList(timestampNamed, renamed, legacyCustom, legacyAutomatic)) {
            Files.setLastModifiedTime(file.toPath(), FileTime.fromMillis(5_000_000));
        }

        assertTrue("Metadata marks timestamp-shaped names as custom", entry(timestampNamed).isCustom());
        assertEquals("Metadata time wins over mtime", created, entry(timestampNamed).createdAt());
        assertEquals(WORLD, entry(timestampNamed).world());
        assertFalse("Metadata marks renamed automatic backups as automatic", entry(renamed).isCustom());
        assertTrue("Without metadata, non-timestamp names are custom", entry(legacyCustom).isCustom());
        assertFalse(entry(legacyAutomatic).isCustom());
        assertEquals(
                "Without metadata, timestamp names give the creation time",
                LocalDateTime.of(2026, 10, 5, 12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                entry(legacyAutomatic).createdAt());
        assertTrue(ArchiveStore.isCustomWorldArchive(timestampNamed));
        assertFalse(ArchiveStore.isCustomWorldArchive(renamed));
    }

    @Test
    public void read_LegacyArchive_UsesUniverseUuid() throws IOException {
        File legacy = new File(backups, "legacy.zip");
        File invalid = new File(backups, "invalid-uuid.zip");
        BackupFixtures.writeLegacyArchive(legacy, "saves/world", UUID.fromString(WORLD));
        BackupFixtures.writeLegacyArchive(invalid, "saves/world", "not-a-uuid");

        assertEquals("Matches the metadata form of the same world", WORLD, entry(legacy).world());
        assertNull(entry(legacy).problem());
        assertNotNull(entry(invalid).problem());
    }

    @Test
    public void read_InvalidMetadata_HasProblem() throws IOException {
        metadataZip("unsupported-version.zip", "version=99\n".getBytes(StandardCharsets.UTF_8));
        metadataZip("invalid-custom.zip", BackupFixtures.metadata(WORLD, 1_000, "not-a-boolean"));
        metadataZip("missing-custom.zip", BackupFixtures.metadata(WORLD, 1_000, null));

        List<ArchiveEntry> entries = new ArchiveStore(backups).list();

        assertEquals(3, entries.size());
        for (ArchiveEntry entry : entries) {
            assertNotNull(entry.id(), entry.problem());
        }
    }

    @Test
    public void read_CorruptedOrOversizedMetadata_HasProblem() throws IOException {
        File corrupted = new File(backups, "corrupted-metadata.zip");
        byte[] contents = BackupFixtures.metadata(WORLD, 1_000, "false");
        CRC32 checksum = new CRC32();
        checksum.update(contents);
        ZipEntry metadata = new ZipEntry(ICompress.BACKUP_METADATA_ENTRY);
        metadata.setMethod(ZipEntry.STORED);
        metadata.setSize(contents.length);
        metadata.setCrc(checksum.getValue());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(corrupted.toPath()))) {
            zip.setComment("world");
            zip.putNextEntry(metadata);
            zip.write(contents);
            zip.putNextEntry(new ZipEntry("saves/world/level.dat"));
            zip.write(LEVEL);
        }
        // Flip one stored byte so the entry no longer matches its recorded CRC.
        byte[] archive = Files.readAllBytes(corrupted.toPath());
        int index = new String(archive, StandardCharsets.ISO_8859_1).indexOf("worldId=") + "worldId=".length();
        archive[index] = '2';
        Files.write(corrupted.toPath(), archive);
        File oversized = metadataZip("oversized-metadata.zip", new byte[16_385]);

        assertTrue(entry(corrupted).problem().contains("checksum"));
        assertTrue(entry(oversized).problem().contains("too large"));
        assertThrows(IOException.class, () -> ArchiveStore.isCustomWorldArchive(corrupted));
        assertThrows(IOException.class, () -> ArchiveStore.isCustomWorldArchive(oversized));
    }

    @Test
    public void writeMetadata_BothCompressorsAtLevelZero() throws Exception {
        ServerUtilitiesConfig.backups.compression_level = 0;
        for (ICompress compressor : Arrays.asList(new LegacyCompressor(), new CommonsCompressor())) {
            File file = new File(build, compressor.getClass().getSimpleName() + ".zip");
            try (ICompress output = compressor) {
                output.createOutputStream(file);
                ArchiveStore.writeMetadata(output, WORLD, 1_000, true);
            }

            try (ZipFile zip = new ZipFile(file)) {
                Properties properties = new Properties();
                properties.load(zip.getInputStream(zip.getEntry(ICompress.BACKUP_METADATA_ENTRY)));
                assertEquals(WORLD, properties.getProperty("worldId"));
                assertEquals("1000", properties.getProperty("createdAt"));
                assertEquals("true", properties.getProperty("customName"));
            }
        }
    }

    private ArchiveEntry entry(File file) throws IOException {
        return new ArchiveStore(backups).list().stream().filter(entry -> entry.file.equals(file)).findFirst()
                .orElseThrow(() -> new AssertionError("Not listed: " + file));
    }

    private File zip(String name, String comment, Map<String, byte[]> entries) throws IOException {
        File file = new File(backups, name);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file.toPath()))) {
            if (comment != null) zip.setComment(comment);
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
            }
        }
        return file;
    }

    private File metadataZip(String name, byte[] metadata) throws IOException {
        Map<String, byte[]> entries = metadataEntry(metadata);
        entries.put("saves/world/level.dat", LEVEL);
        return zip(name, "world", entries);
    }

    private static Map<String, byte[]> metadataEntry(byte[] metadata) {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put(ICompress.BACKUP_METADATA_ENTRY, metadata);
        return entries;
    }

    private static Map<String, byte[]> levels(String... names) {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        for (String name : names) entries.put(name, LEVEL);
        return entries;
    }
}
