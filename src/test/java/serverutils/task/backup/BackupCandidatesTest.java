package serverutils.task.backup;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class BackupCandidatesTest extends BackupFixtures.Tests {

    @Before
    public void beforeEach() {
        testSetup(SetupFlags.FOLDERS);
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
}
