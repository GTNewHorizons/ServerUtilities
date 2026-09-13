package serverutils.lib.util.compression;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ArchiveExtractionTest {

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void includePatternsCannotWriteIntoThePreservedWorld() throws Exception {
        String[] previous = serverutils.ServerUtilitiesConfig.backups.additional_backup_files;
        serverutils.ServerUtilitiesConfig.backups.additional_backup_files = new String[] { "saves/**" };
        try {
            Path root = temporary.newFolder().toPath();
            Path preserved = root.resolve("saves/world_old");
            Files.createDirectories(preserved);
            Path intact = preserved.resolve("level.dat");
            Files.write(intact, "preserved".getBytes(StandardCharsets.UTF_8));
            Path archive = archive("saves/world/level.dat", "saves/world_old/level.dat");
            assertThrows(
                    IOException.class,
                    () -> ArchiveExtraction.extract(archive.toFile(), true, false, root, preserved.toFile()));
            assertEquals("preserved", new String(Files.readAllBytes(intact), StandardCharsets.UTF_8));
            assertFalse(Files.exists(root.resolve("saves/world/level.dat")));
        } finally {
            serverutils.ServerUtilitiesConfig.backups.additional_backup_files = previous;
        }
    }

    @Test
    public void restorePreflightRequiresSelectedWorldMetadata() throws Exception {
        String[] previous = serverutils.ServerUtilitiesConfig.backups.additional_backup_files;
        serverutils.ServerUtilitiesConfig.backups.additional_backup_files = new String[] { "**" };
        try {
            for (boolean dedicated : new boolean[] { false, true }) {
                String prefix = dedicated ? "world/" : "saves/world/";
                for (boolean global : new boolean[] { false, true }) {
                    for (String[] contents : new String[][] { {},
                            { serverutils.ServerUtilities.SERVER_FOLDER + "ranks.txt" },
                            { "saves/other-world/level.dat" }, { prefix + "region/r.0.0.mca" },
                            { prefix + "level.dat/" } }) {
                        Path archive = archiveWithComment("world", contents);
                        IOException failure = assertThrows(
                                IOException.class,
                                () -> ICompress.validateRestoreTargets(archive.toFile(), "world", dedicated, global));
                        assertTrue(failure.getMessage().contains("no level.dat"));
                    }
                    for (String metadata : new String[] { "level.dat", "level.dat_old" }) {
                        Path archive = archiveWithComment("world", prefix + metadata);
                        ICompress.validateRestoreTargets(archive.toFile(), "world", dedicated, global);
                    }
                }
            }
        } finally {
            serverutils.ServerUtilitiesConfig.backups.additional_backup_files = previous;
        }
    }

    @Test
    public void dedicatedArchiveKeepsGlobalFilesOutsideSaves() throws Exception {
        for (boolean dedicated : new boolean[] { true, false }) {
            for (boolean extra : new boolean[] { false, true }) {
                checkWorldLayout(dedicated, extra);
            }
        }
    }

    private void checkWorldLayout(boolean dedicated, boolean extra) throws Exception {
        serverutils.ServerUtilitiesConfig.backups.additional_backup_files = new String[] { "saves/NEI/global/**" };
        Path root = temporary.newFolder().toPath();
        Path archive = temporary.newFile().toPath();
        String ranks = serverutils.ServerUtilities.SERVER_FOLDER + "ranks.txt";
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.setComment("world");
            for (String name : new String[] { (dedicated ? "" : "saves/") + "world/level.dat", ranks }) {
                zip.putNextEntry(new ZipEntry(name));
                zip.write("new".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            if (extra) {
                zip.putNextEntry(new ZipEntry("saves/NEI/global/settings.dat"));
                zip.write("extra".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        try {
            for (ICompress compressor : new ICompress[] { new LegacyCompressor(), new CommonsCompressor() }) {
                boolean legacy = compressor.isOldBackup(archive.toFile());
                assertEquals(dedicated, legacy);
                ICompress.validateRestoreTargets(archive.toFile(), "world", legacy);
                ArchiveExtraction.extract(archive.toFile(), true, legacy, root);
                assertTrue(Files.exists(root.resolve("saves/world/level.dat")));
                assertTrue(Files.exists(root.resolve(ranks)));
                assertFalse(Files.exists(root.resolve("saves/" + ranks)));
                assertEquals(extra, Files.exists(root.resolve("saves/NEI/global/settings.dat")));
            }
        } finally {
            serverutils.ServerUtilitiesConfig.backups.additional_backup_files = new String[0];
        }
    }

    @Test
    public void successfulGlobalRestoreRetainsOriginalRanks() throws Exception {
        Path root = temporary.newFolder().toPath();
        String ranks = serverutils.ServerUtilities.SERVER_FOLDER + "ranks.txt";
        Path target = root.resolve(ranks);
        Files.createDirectories(target.getParent());
        Files.write(target, "original".getBytes(StandardCharsets.UTF_8));
        ArchiveExtraction.extract(archive(ranks).toFile(), true, false, root);
        assertEquals("new", new String(Files.readAllBytes(target), StandardCharsets.UTF_8));
        try (java.util.stream.Stream<Path> copies = Files.walk(root.resolve("backups_before_restore"))) {
            Path copy = copies.filter(path -> path.endsWith(ranks)).findFirst().get();
            assertEquals("original", new String(Files.readAllBytes(copy), StandardCharsets.UTF_8));
        }
    }

    @Test
    public void replacedFilesGoToTheSuppliedRecoveryDirectory() throws Exception {
        Path root = temporary.newFolder().toPath();
        String ranks = serverutils.ServerUtilities.SERVER_FOLDER + "ranks.txt";
        Path target = root.resolve(ranks);
        Files.createDirectories(target.getParent());
        Files.write(target, "original".getBytes(StandardCharsets.UTF_8));
        Path recovery = root.resolve("backups_before_restore/2026-01-01-00-00-00-abc");
        ArchiveExtraction.extract(archive(ranks).toFile(), true, false, root, null, recovery.toFile());
        assertEquals(
                "original",
                new String(Files.readAllBytes(recovery.resolve("files").resolve(ranks)), StandardCharsets.UTF_8));
        try (java.util.stream.Stream<Path> entries = Files.list(root.resolve("backups_before_restore"))) {
            assertEquals(1, entries.count());
        }
    }

    @Test
    public void worldOnlyRestoreSurvivesChangedAdditionalFileConfig() throws Exception {
        serverutils.ServerUtilitiesConfig.backups.additional_backup_files = new String[0];
        Path root = temporary.newFolder().toPath();
        Path oldGlobal = root.resolve("old-config/settings.dat");
        Files.createDirectories(oldGlobal.getParent());
        Files.write(oldGlobal, "keep".getBytes(StandardCharsets.UTF_8));
        Path archive = archiveWithComment(
                "world",
                "saves/world/level.dat",
                "old-config/settings.dat",
                "saves/world_old/level.dat");
        ICompress.validateRestoreTargets(archive.toFile(), "world", false, false);
        assertThrows(IOException.class, () -> ICompress.validateRestoreTargets(archive.toFile(), "world", false, true));
        ArchiveExtraction.extract(archive.toFile(), false, false, root);
        assertTrue(Files.isRegularFile(root.resolve("saves/world/level.dat")));
        assertEquals("keep", new String(Files.readAllBytes(oldGlobal), StandardCharsets.UTF_8));
        assertFalse(Files.exists(root.resolve("saves/world_old")));
        Path unsafe = archiveWithComment("world", "saves/world/level.dat", "../escape");
        assertThrows(IOException.class, () -> ArchiveExtraction.extract(unsafe.toFile(), false, false, root));

        // A broad global pattern must not turn the selected world's own files into excluded globals.
        serverutils.ServerUtilitiesConfig.backups.additional_backup_files = new String[] { "saves/**" };
        try {
            Path otherRoot = temporary.newFolder().toPath();
            ArchiveExtraction.extract(archive.toFile(), false, false, otherRoot);
            assertTrue(Files.isRegularFile(otherRoot.resolve("saves/world/level.dat")));
            assertFalse(Files.exists(otherRoot.resolve("saves/world_old")));
        } finally {
            serverutils.ServerUtilitiesConfig.backups.additional_backup_files = new String[0];
        }
    }

    @Test
    public void corruptStoredPayloadIsRejectedBeforeReplacement() throws Exception {
        Path root = temporary.newFolder().toPath();
        Files.write(root.resolve("value"), "original".getBytes(StandardCharsets.UTF_8));
        Path archive = temporary.newFile().toPath();
        byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(payload);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            ZipEntry entry = new ZipEntry("value");
            entry.setMethod(ZipEntry.STORED);
            entry.setSize(payload.length);
            entry.setCrc(crc.getValue());
            zip.putNextEntry(entry);
            zip.write(payload);
            zip.closeEntry();
        }
        byte[] bytes = Files.readAllBytes(archive);
        int payloadOffset = 30 + (bytes[26] & 255)
                + ((bytes[27] & 255) << 8)
                + (bytes[28] & 255)
                + ((bytes[29] & 255) << 8);
        bytes[payloadOffset] ^= 1;
        Files.write(archive, bytes);
        try {
            ArchiveExtraction.extract(archive.toFile(), true, false, root);
            fail("Accepted corrupt ZIP contents");
        } catch (IOException expected) {}
        assertEquals("original", new String(Files.readAllBytes(root.resolve("value")), StandardCharsets.UTF_8));
    }

    @Test
    public void dottedPatternsValidateAndExcludeGlobalFiles() throws Exception {
        serverutils.ServerUtilitiesConfig.backups.additional_backup_files = new String[] { "./saves/./NEI/global/**" };
        try {
            Path root = temporary.newFolder().toPath();
            Path archive = archive("saves/world/level.dat", "saves/NEI/global/settings.dat");
            ArchiveExtraction.validateRestoreTargets(archive.toFile(), "world", false);
            ArchiveExtraction.extract(archive.toFile(), false, false, root);
            assertTrue(Files.exists(root.resolve("saves/world/level.dat")));
            assertFalse(Files.exists(root.resolve("saves/NEI/global/settings.dat")));
        } finally {
            serverutils.ServerUtilitiesConfig.backups.additional_backup_files = new String[0];
        }
    }

    @Test
    public void invalidEntryCannotOverwriteEarlierDestination() throws Exception {
        for (String bad : new String[] { "../escape", "/absolute", "C:/absolute", "a/../../escape", "a/../value" }) {
            Path root = temporary.newFolder().toPath();
            Files.write(root.resolve("value"), "old".getBytes(StandardCharsets.UTF_8));
            Path zip = archive("value", bad);
            try {
                ArchiveExtraction.extract(zip.toFile(), true, false, root);
                fail("Accepted " + bad);
            } catch (IOException expected) {}
            assertEquals("old", new String(Files.readAllBytes(root.resolve("value")), StandardCharsets.UTF_8));
        }
    }

    @Test
    public void failedInstallRollsBackPreviousFiles() throws Exception {
        Path root = temporary.newFolder().toPath();
        Files.write(root.resolve("value"), "old".getBytes(StandardCharsets.UTF_8));
        Files.createDirectory(root.resolve("blocked"));
        try {
            ArchiveExtraction.extract(archive("value", "created", "blocked").toFile(), true, false, root);
            fail("Expected a directory conflict");
        } catch (IOException expected) {}
        assertEquals("old", new String(Files.readAllBytes(root.resolve("value")), StandardCharsets.UTF_8));
        assertFalse(Files.exists(root.resolve("created")));
        assertTrue(Files.isDirectory(root.resolve("blocked")));
    }

    @Test
    public void failedInstallRemovesDirectoriesItCreated() throws Exception {
        Path root = temporary.newFolder().toPath();
        Files.createDirectory(root.resolve("blocked"));
        try {
            ArchiveExtraction.extract(archive("displaced/deep/value", "blocked").toFile(), true, false, root);
            fail("Expected a directory conflict");
        } catch (IOException expected) {}
        assertFalse(Files.exists(root.resolve("displaced")));
    }

    @Test
    public void restoreCannotOverwriteThePreservedWorld() throws Exception {
        serverutils.ServerUtilitiesConfig.backups.additional_backup_files = new String[0];
        try {
            ArchiveExtraction.validateRestoreTargets(archive("saves/world_old/level.dat").toFile(), "world", false);
            fail("Accepted a different world");
        } catch (IOException expected) {}
        ArchiveExtraction.validateRestoreTargets(archive("saves/world/level.dat").toFile(), "world", false);
    }

    @Test
    public void duplicateNormalizedEntriesAreRejected() throws Exception {
        Path root = temporary.newFolder().toPath();
        try {
            ArchiveExtraction.extract(archive("value", "./value").toFile(), true, false, root);
            fail("Expected duplicate entry rejection");
        } catch (IOException expected) {}
        assertFalse(Files.exists(root.resolve("value")));
    }

    @Test
    public void worldOnlyRestoreDoesNotReplaceGlobalRanks() throws Exception {
        Path root = temporary.newFolder().toPath();
        String ranks = serverutils.ServerUtilities.SERVER_FOLDER + "ranks.txt";
        serverutils.ServerUtilitiesConfig.backups.additional_backup_files = new String[0];
        Path zip = archive("saves/world/level.dat", ranks);
        ArchiveExtraction.extract(zip.toFile(), false, false, root);
        assertTrue(Files.isRegularFile(root.resolve("saves/world/level.dat")));
        assertFalse(Files.exists(root.resolve(ranks)));
        ArchiveExtraction.extract(zip.toFile(), true, false, root);
        assertTrue(Files.isRegularFile(root.resolve(ranks)));
    }

    @Test
    public void legacyArchiveUsesSavesPrefix() throws Exception {
        Path root = temporary.newFolder().toPath();
        ArchiveExtraction.extract(archive("world/level.dat").toFile(), true, true, root);
        assertTrue(Files.isRegularFile(root.resolve("saves/world/level.dat")));
    }

    private Path archive(String... names) throws Exception {
        return archiveWithComment(null, names);
    }

    private Path archiveWithComment(String worldName, String... names) throws Exception {
        Path archive = temporary.newFile().toPath();
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            if (worldName != null) zip.setComment(worldName);
            for (String name : names) {
                zip.putNextEntry(new ZipEntry(name));
                zip.write("new".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return archive;
    }
}
