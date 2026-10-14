package serverutils.task.backup;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class BackupCandidatesTest extends BackupFixtures.Tests {

    @Before
    public void beforeEach() {
        testSetup(SetupFlags.FOLDERS);
    }

    @After
    public void afterEach() {
        testReset();
    }

    @Test
    public void scan_WhenWorldIsInBackupStorage_ShouldThrowException() {
        IOException exception = assertThrows(IOException.class, () -> BackupCandidates.scanWorld(world, saves, false));
        assertEquals(
                "Backup and temporary storage must not contain the world directory: " + world,
                exception.getMessage());
    }

    @Test
    public void scan_WhenWorldDoesntExist_ShouldThrowException() {
        File someDir = new File(build, UUID.randomUUID().toString());
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> BackupCandidates.scanWorld(someDir, backups, false));
        assertEquals("World does not exist: " + someDir, exception.getMessage());
    }

    @Test
    public void scan_WhenWorldIsInvalid_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> BackupCandidates.scanWorld(saves, backups, false));
        assertEquals("World is invalid: " + saves, exception.getMessage());
    }

    @Test
    public void scan_WhenBackupStorageInsideWorld_SkipIt() throws IOException {
        File nestedBackups = new File(world, "backups");
        File archived = new File(nestedBackups, "deep/old.zip");
        File keep = new File(world, "keep.dat");
        Files.createDirectories(archived.getParentFile().toPath());
        Files.write(archived.toPath(), new byte[] { 1 });
        Files.write(keep.toPath(), new byte[] { 2 });

        Set<File> files = files(BackupCandidates.scanWorld(world, nestedBackups, false));

        assertTrue(files.contains(keep));
        assertFalse(files.contains(archived));
    }

    @Test
    public void scan_WhenFileLinksIntoBackupStorage_SkipUt() throws IOException {
        File target = new File(backups, "target.zip");
        File link = new File(world, "linked-backup.zip");
        Files.write(target.toPath(), new byte[] { 1 });
        try {
            Files.createSymbolicLink(link.toPath(), target.toPath().toAbsolutePath());
        } catch (IOException | UnsupportedOperationException | SecurityException unsupported) {
            Assume.assumeNoException(unsupported);
        }

        Set<File> files = files(BackupCandidates.scanWorld(world, backups, false));

        assertFalse(files.contains(link));
    }

    private static Set<File> files(BackupCandidates candidates) {
        return candidates.getFiles().stream().map(WorldFile::backing).collect(Collectors.toSet());
    }
}
