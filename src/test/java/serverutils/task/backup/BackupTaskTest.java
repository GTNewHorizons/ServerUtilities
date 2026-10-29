package serverutils.task.backup;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.time.Instant;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import serverutils.ServerUtilitiesConfig;

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
        testReset();
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
