package serverutils.task.backup;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import serverutils.ServerUtilitiesConfig;
import serverutils.lib.util.FileUtils;

public class BackupTaskTest extends BackupFixtures.Tests {

    private MockedStatic<BackupCoordinator> coordinator;
    private BackupTask task;

    @Before
    public void beforeEach() throws ReflectiveOperationException {
        testSetup();

        BackupFixtures.setStatic(BackupTask.class, "lastPlayerActivity", Instant.MIN);
        BackupFixtures.setStatic(BackupRoutine.class, "lastSuccessfulBackup", Instant.MIN);

        task = new BackupTask();

        coordinator = mockStatic(BackupCoordinator.class);
        coordinator.when(() -> BackupCoordinator.startBackup(any())).thenReturn(true);
    }

    @After
    public void afterEach() {
        coordinator.close();
    }

    @Test
    public void deleteAbandonedArchives_DeletesOnlyStagingFiles() throws IOException {
        File folder = FileUtils.ensureExists(new File(build, "staging"));
        File orphan = write(new File(folder, ".su-save-123.tmp"));
        File archive = write(new File(folder, "completed.zip"));
        File unrelated = write(new File(folder, "other.tmp"));
        File contents = write(new File(folder, ".su-save-directory.tmp/keep"));

        BackupTask.deleteAbandonedArchives(folder);

        assertFalse(orphan.exists());
        assertTrue(archive.exists());
        assertTrue(unrelated.exists());
        assertTrue(contents.exists());

        // nothing left to reclaim, so a second run changes nothing
        BackupTask.deleteAbandonedArchives(folder);
        assertTrue(archive.exists());
    }

    private static File write(File file) throws IOException {
        FileUtils.ensureExists(file.getParentFile());
        Files.write(file.toPath(), new byte[] { 1 });
        return file;
    }

    @Test
    public void execute_whenNothingRunning_ShouldStartBackup() {
        task.execute(universe);
        coordinator.verify(() -> BackupCoordinator.startBackup(any()), times(1));
    }

    @Test
    public void execute_whenBackupIsRunning_ShouldDiscard() {
        coordinator.when(() -> BackupCoordinator.startBackup(any())).thenReturn(false);
        task.execute(universe);
        coordinator.verify(() -> BackupCoordinator.startBackup(any()), times(1));
    }

    @Test
    public void execute_whenBackupsDisabled_ShouldSkip() {
        ServerUtilitiesConfig.backups.enable_backups = false;

        task.execute(universe);
        coordinator.verify(() -> BackupCoordinator.startBackup(any()), never());
    }

    @Test
    public void execute_whenRequiresPlayer_andNoPlayerActivity_ShouldSkip() {
        ServerUtilitiesConfig.backups.need_online_players = true;

        task.execute(universe);
        coordinator.verify(() -> BackupCoordinator.startBackup(any()), never());
    }

    @Test
    public void execute_whenRequiresPlayer_andNoActivitySinceLastBackup_ShouldSkip()
            throws ReflectiveOperationException {
        ServerUtilitiesConfig.backups.need_online_players = true;
        BackupTask.newPlayerActivity();
        BackupFixtures.setStatic(BackupRoutine.class, "lastSuccessfulBackup", Instant.now().plusSeconds(60));

        task.execute(universe);
        coordinator.verify(() -> BackupCoordinator.startBackup(any()), never());
    }

    @Test
    public void execute_whenRequiresPlayer_andRecentActivity_ShouldStartBackup() {
        ServerUtilitiesConfig.backups.need_online_players = true;
        BackupTask.newPlayerActivity();

        task.execute(universe);
        coordinator.verify(() -> BackupCoordinator.startBackup(any()), times(1));
    }

    @Test
    public void execute_whenRequiresPlayer_andPlayersOnline_ShouldStartBackup() {
        ServerUtilitiesConfig.backups.need_online_players = true;
        addPlayer();

        task.execute(universe);
        coordinator.verify(() -> BackupCoordinator.startBackup(any()), times(1));
    }
}
