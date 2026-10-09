package serverutils.task.backup;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import serverutils.ServerUtilitiesConfig;
import serverutils.lib.util.compression.ICompress;

public class LegacyBackupRetentionTest {

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder(new File("build"));

    @BeforeClass
    public static void configureBackups() {
        ServerUtilitiesConfig.backups.backup_folder_path = "build/test-backups";
    }

    private Path file(String name, int size, long modified) throws Exception {
        Path file = temporary.getRoot().toPath().resolve(name);
        if (name.endsWith(".zip")) {
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file))) {
                zip.setComment("world");
                zip.putNextEntry(new ZipEntry("world/level.dat"));
                zip.write(new byte[size]);
                zip.putNextEntry(new ZipEntry("world/serverutilities/universe.dat"));
                net.minecraft.nbt.NBTTagCompound universe = new net.minecraft.nbt.NBTTagCompound();
                universe.setString(
                        "UUID",
                        serverutils.lib.util.StringUtils
                                .fromUUID(java.util.UUID.fromString("12345678-1234-1234-1234-123456789abc")));
                java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
                net.minecraft.nbt.CompressedStreamTools.writeCompressed(universe, output);
                zip.write(output.toByteArray());
            }
        } else Files.write(file, new byte[size]);
        Files.setLastModifiedTime(file, FileTime.fromMillis(modified));
        return file;
    }

    private static void clearLegacyBackups(File folder, int count, long size, boolean deleteCustom) throws Exception {
        BackupTask.clearLegacyBackups(BackupTask.readLegacyBackups(folder, deleteCustom), count, size);
    }

    private Path metadataArchive(String name, String custom, long modified) throws Exception {
        Path file = temporary.getRoot().toPath().resolve(name);
        Properties metadata = new Properties();
        metadata.setProperty("version", "1");
        metadata.setProperty("worldId", "12345678-1234-1234-1234-123456789abc");
        metadata.setProperty("createdAt", "123");
        if (custom != null) metadata.setProperty("customName", custom);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file))) {
            zip.setComment("world");
            zip.putNextEntry(new ZipEntry("world/level.dat"));
            zip.write(1);
            zip.putNextEntry(new ZipEntry(ICompress.BACKUP_METADATA_ENTRY));
            metadata.store(zip, null);
        }
        Files.setLastModifiedTime(file, FileTime.fromMillis(modified));
        return file;
    }

    @Test
    public void preservesLatestPerWorldWithCountAndSizeLimitsIncludingUncommentedArchives() throws Exception {
        for (long cap : new long[] { 0, 1 }) {
            for (String name : new String[] { "a-old", "a-latest", "b-old", "b-latest" }) {
                Path path = temporary.getRoot().toPath().resolve(name + ".zip");
                try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(path))) {
                    zip.putNextEntry(new ZipEntry("worlds/" + name.charAt(0) + "/level.dat"));
                    zip.write(1);
                }
                Files.setLastModifiedTime(path, FileTime.fromMillis(name.endsWith("latest") ? 2 : 1));
            }
            clearLegacyBackups(temporary.getRoot(), 1, cap, true);
            for (String name : new String[] { "a", "b" }) {
                assertTrue(Files.exists(temporary.getRoot().toPath().resolve(name + "-latest.zip")));
                assertFalse(Files.exists(temporary.getRoot().toPath().resolve(name + "-old.zip")));
            }
        }
    }

    @Test
    public void futureMetadataAndProtectedCustomArchivesCannotDisplaceLatestAutomaticBackup() throws Exception {
        Path latest = metadataArchive("latest.zip", "false", 2);
        Path custom = metadataArchive("checkpoint.zip", "true", 3);
        Path future = temporary.getRoot().toPath().resolve("future.zip");
        Properties metadata = new Properties();
        metadata.setProperty("version", "1");
        metadata.setProperty("worldId", "12345678-1234-1234-1234-123456789abc");
        metadata.setProperty("createdAt", Long.toString(Long.MAX_VALUE));
        metadata.setProperty("customName", "false");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(future))) {
            zip.putNextEntry(new ZipEntry("world/level.dat"));
            zip.write(1);
            zip.putNextEntry(new ZipEntry(ICompress.BACKUP_METADATA_ENTRY));
            metadata.store(zip, null);
        }
        Files.setLastModifiedTime(future, FileTime.fromMillis(4));
        for (long cap : new long[] { 0, 1 }) {
            Path old = metadataArchive("old.zip", "false", 1);
            clearLegacyBackups(temporary.getRoot(), 1, cap, false);
            assertFalse(Files.exists(old));
            assertTrue(Files.exists(latest));
            assertTrue(Files.exists(custom));
            assertTrue(Files.exists(future));
        }
    }

    @Test
    public void metadataOverridesFilenameProtectionInBothRotationModes() throws Exception {
        for (long cap : new long[] { 0, 1 }) {
            for (boolean deleteCustom : new boolean[] { false, true }) {
                Path old = metadataArchive("renamed-automatic.zip", "false", 1);
                Path latest = metadataArchive("latest.zip", "false", 2);
                Path custom = metadataArchive("2026-10-07-12-00-00-milestone.zip", "true", 0);
                Path legacyCustom = file("pre-metadata-milestone.zip", 10, 0);
                clearLegacyBackups(temporary.getRoot(), 1, cap, deleteCustom);
                assertFalse("Metadata identifies renamed automatic backups as eligible", Files.exists(old));
                assertTrue("Rotation still orders archives by mtime", Files.exists(latest));
                org.junit.Assert.assertEquals(
                        "Metadata protects timestamp-shaped custom names",
                        !deleteCustom,
                        Files.exists(custom));
                org.junit.Assert.assertEquals(
                        "Archives without metadata keep filename-based protection",
                        !deleteCustom,
                        Files.exists(legacyCustom));
            }
        }
    }

    @Test
    public void malformedMetadataCannotDisplaceUsableHistoryInEitherRotationMode() throws Exception {
        Path invalid = metadataArchive("2026-10-07-12-00-00-invalid.zip", "not-a-boolean", 3);
        Path missing = metadataArchive("2026-10-07-12-00-00-missing.zip", null, 3);
        for (long cap : new long[] { 0, 1 }) {
            for (boolean deleteCustom : new boolean[] { false, true }) {
                Path old = metadataArchive("old.zip", "false", 1);
                Path latest = metadataArchive("latest.zip", "false", 2);
                clearLegacyBackups(temporary.getRoot(), 1, cap, deleteCustom);
                assertFalse(Files.exists(old));
                assertTrue("Malformed metadata must not replace the last eligible backup", Files.exists(latest));
                assertTrue(Files.exists(invalid));
                assertTrue(Files.exists(missing));
            }
        }
    }

    @Test
    public void cancelledScanLeavesAllHistoryUntouched() throws Exception {
        Path old = file("old.zip", 10, 1);
        Path latest = file("latest.zip", 1, 2);
        Thread.currentThread().interrupt();
        try {
            org.junit.Assert.assertThrows(
                    java.io.InterruptedIOException.class,
                    () -> clearLegacyBackups(temporary.getRoot(), 1, 0, true));
        } finally {
            Thread.interrupted();
        }
        java.util.List<BackupRetention.Archive> candidates = BackupTask.readLegacyBackups(temporary.getRoot(), true);
        Thread.currentThread().interrupt();
        try {
            org.junit.Assert.assertThrows(
                    java.io.InterruptedIOException.class,
                    () -> BackupTask.clearLegacyBackups(candidates, 1, 0));
        } finally {
            Thread.interrupted();
        }
        assertTrue(Files.exists(old));
        assertTrue(Files.exists(latest));
    }

    @Test
    public void newerMalformedArchivesCannotDisplaceUsableHistory() throws Exception {
        for (long cap : new long[] { 0, 1 }) {
            Path old = file("old.zip", 10, 1);
            Path latest = file("latest.zip", 1, 2);
            Path broken = Files.write(temporary.getRoot().toPath().resolve("broken.zip"), new byte[1024]);
            Path metadataOnly = temporary.getRoot().toPath().resolve("metadata-only.zip");
            Path foreign = temporary.getRoot().toPath().resolve("foreign.zip");
            for (Path path : new Path[] { metadataOnly, foreign }) {
                try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(path))) {
                    zip.setComment("world");
                    zip.putNextEntry(
                            new ZipEntry(
                                    path.equals(metadataOnly)
                                            ? serverutils.lib.util.compression.ICompress.BACKUP_METADATA_ENTRY
                                            : "unrelated.txt"));
                    zip.write(1);
                }
            }
            for (Path path : new Path[] { broken, metadataOnly, foreign }) {
                Files.setLastModifiedTime(path, FileTime.fromMillis(3));
            }
            clearLegacyBackups(temporary.getRoot(), 1, cap, true);
            assertFalse(Files.exists(old));
            assertTrue("Malformed ZIPs must not displace the last recognized world backup", Files.exists(latest));
            for (Path path : new Path[] { broken, metadataOnly, foreign }) {
                assertTrue("Unknown ZIPs must be preserved, not counted as backup history", Files.exists(path));
            }
        }
    }

    @Test
    public void onlyRegularZipFilesParticipateInEitherLegacyMode() throws Exception {
        for (long limit : new long[] { 0, 1 }) {
            Path folder = Files.createDirectory(temporary.getRoot().toPath().resolve("directory-" + limit + ".zip"));
            Path contents = Files.write(folder.resolve("world.dat"), new byte[] { 1 });
            Path unrelated = file("notes-" + limit + ".txt", 10, 1);
            Path timestamped = file("2020-01-01-00-00-00-not-a-zip-" + limit + ".dat", 10, 1);
            Path old = file("old-" + limit + ".zip", 10, 2);
            Path latest = file("latest-" + limit + ".zip", 1, 3);
            clearLegacyBackups(temporary.getRoot(), 1, limit, true);
            assertTrue("Pruning must not recurse into directories", Files.exists(contents));
            assertTrue("Unrelated files must survive", Files.exists(unrelated));
            assertTrue("A timestamp prefix does not make a file a ZIP", Files.exists(timestamped));
            assertFalse(Files.exists(old));
            assertTrue(Files.exists(latest));
        }
    }

    @Test
    public void oversizeNewestAndInvalidCountsNeverRemoveTheLastArchive() throws Exception {
        Path old = file("old.zip", 10, 1);
        Path newest = file("newest.zip", 10, 2);
        clearLegacyBackups(temporary.getRoot(), 1, 1, true);
        assertFalse(Files.exists(old));
        assertTrue("Keep the newest archive even if it exceeds the size cap", Files.exists(newest));
        for (int count : new int[] { 0, -1 }) {
            clearLegacyBackups(temporary.getRoot(), count, 0, true);
            assertTrue(Files.exists(newest));
        }
    }

    @Test
    public void failedDeletionDoesNotCountAsFreedSpaceOrRemovedArchive() throws Exception {
        for (boolean sizeMode : new boolean[] { true, false }) {
            Path undeletable = file("undeletable.zip", 8, 1);
            Path middle = file("middle.zip", 8, 2);
            Path latest = file("latest.zip", 1, 3);
            Path failure = Files.createDirectories(temporary.getRoot().toPath().resolve("nonempty"));
            Files.write(failure.resolve("keep"), new byte[] { 1 });
            File old = mock(File.class);
            when(old.getName()).thenReturn("undeletable.zip");
            when(old.getPath()).thenReturn(undeletable.toString());
            when(old.lastModified()).thenReturn(1L);
            when(old.length()).thenReturn(Files.size(undeletable));
            when(old.exists()).thenReturn(true);
            when(old.isFile()).thenReturn(true);
            when(old.toPath()).thenReturn(undeletable);
            File folder = mock(File.class);
            when(folder.listFiles()).thenReturn(new File[] { old, middle.toFile(), latest.toFile() });
            long cap = sizeMode ? Files.size(undeletable) + Files.size(latest) : 0;
            java.util.List<BackupRetention.Archive> candidates = BackupTask.readLegacyBackups(folder, true);
            // Simulate the filesystem refusing removal after the candidate was verified.
            when(old.toPath()).thenReturn(failure);
            BackupTask.clearLegacyBackups(candidates, 1, cap);
            assertTrue(Files.exists(undeletable));
            assertFalse("Try another old archive when the first deletion fails", Files.exists(middle));
            assertTrue(Files.exists(latest));
        }
    }

    @Test
    public void customProtectionAndEqualTimeOrderingWorkInBothModes() throws Exception {
        for (long cap : new long[] { 0, 1 }) {
            Path a = file("2020-01-01-00-00-00.zip", 8, 1);
            Path b = file("2020-01-01-00-00-01.zip", 8, 1);
            Path c = file("2020-01-01-00-00-02.zip", 1, 1);
            Path custom = file("milestone.zip", 10, 0);
            File folder = mock(File.class);
            // Directory enumeration order must not decide which equally dated archive survives.
            when(folder.listFiles()).thenReturn(new File[] { c.toFile(), a.toFile(), custom.toFile(), b.toFile() });
            clearLegacyBackups(folder, 1, cap, false);
            assertFalse(Files.exists(a));
            assertFalse(Files.exists(b));
            assertTrue(Files.exists(c));
            assertTrue(
                    "Protected custom backups do not consume the eligible count/size allowance",
                    Files.exists(custom));
        }
    }

    @Test
    public void negativeSizeLimitSkipsPruning() throws Exception {
        Path old = file("old.zip", 10, 1);
        Path latest = file("latest.zip", 10, 2);
        clearLegacyBackups(temporary.getRoot(), 1, -1, true);
        assertTrue(Files.exists(old));
        assertTrue(Files.exists(latest));
    }

    @Test
    public void positiveSizeLimitStillTakesPrecedenceOverCountLimit() throws Exception {
        Path old = file("old.zip", 10, 1);
        Path latest = file("latest.zip", 1, 2);
        clearLegacyBackups(temporary.getRoot(), 1, Files.size(old) + Files.size(latest), true);
        assertTrue(Files.exists(old));
        assertTrue(Files.exists(latest));
        clearLegacyBackups(temporary.getRoot(), 1, 0, true);
        assertFalse(Files.exists(old));
        assertTrue(Files.exists(latest));
    }

    @Test
    public void fileAndDirectorySymlinksAndTheirTargetsArePreserved() throws Exception {
        Path target = file("target.dat", 10, 0);
        Path directory = Files.createDirectory(temporary.getRoot().toPath().resolve("world"));
        Path contents = Files.write(directory.resolve("keep"), new byte[] { 1 });
        Path fileLink = temporary.getRoot().toPath().resolve("linked.zip");
        Path directoryLink = temporary.getRoot().toPath().resolve("linked-world.zip");
        try {
            Files.createSymbolicLink(fileLink, target.toAbsolutePath());
            Files.createSymbolicLink(directoryLink, directory.toAbsolutePath());
        } catch (java.nio.file.FileSystemException | UnsupportedOperationException ex) {
            org.junit.Assume.assumeNoException("Symbolic links unavailable on this host", ex);
        }
        for (long cap : new long[] { 0, 1 }) {
            Path old = file("old.zip", 10, 1);
            Path latest = file("latest.zip", 1, 2);
            clearLegacyBackups(temporary.getRoot(), 1, cap, true);
            assertFalse(Files.exists(old));
            assertTrue(Files.exists(latest));
            assertTrue(Files.isSymbolicLink(fileLink));
            assertTrue(Files.isSymbolicLink(directoryLink));
            assertTrue(Files.exists(target));
            assertTrue(Files.exists(contents));
        }
    }
}
