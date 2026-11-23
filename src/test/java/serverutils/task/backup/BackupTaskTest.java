package serverutils.task.backup;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import net.minecraft.entity.player.EntityPlayerMP;

import org.junit.After;
import org.junit.Assert;
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
        BackupFixtures.setStatic(BackupTask.class, "pausedUntil", null);
        BackupFixtures.setStatic(BackupRoutine.class, "lastSuccessfulBackup", Instant.MIN);

        task = new BackupTask();

        coordinator = mockStatic(BackupCoordinator.class);
        coordinator.when(() -> BackupCoordinator.startBackup(any())).thenReturn(true);
    }

    @After
    public void afterEach() {
        BackupTask.clock = Clock.systemUTC();
        coordinator.close();
        testReset();
    }

    private static void setTime(Instant now) {
        BackupTask.clock = Clock.fixed(now, ZoneOffset.UTC);
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

    @Test
    public void timedPause_ShouldSkipUntilExpiry_ThenRunOnSchedule() {
        Instant start = Instant.parse("2026-11-23T12:00:00Z");
        setTime(start);
        BackupTask.pauseBackupsFor(6 * 3_600_000L);

        setTime(start.plusSeconds(6 * 3600).minusMillis(1));
        task.execute(universe);
        coordinator.verify(() -> BackupCoordinator.startBackup(any()), never());

        setTime(start.plusSeconds(6 * 3600));
        task.execute(universe);
        coordinator.verify(() -> BackupCoordinator.startBackup(any()), times(1));
        Assert.assertFalse(BackupTask.isPaused());
    }

    @Test
    public void timedPause_ShouldReplaceIndefinitePause_AndViceVersa() {
        Instant start = Instant.parse("2026-11-23T12:00:00Z");
        setTime(start);

        BackupTask.pauseBackups();
        BackupTask.pauseBackupsFor(60_000L);
        setTime(start.plusSeconds(60));
        Assert.assertFalse("timed pause replaced the indefinite one", BackupTask.isPaused());

        BackupTask.pauseBackupsFor(60_000L);
        Assert.assertTrue("indefinite pause replaces a timed one", BackupTask.pauseBackups());
        setTime(start.plusSeconds(3600));
        Assert.assertTrue(BackupTask.isPaused());
    }

    @Test
    public void resume_afterExpiry_ReportsNotPaused() {
        Instant start = Instant.parse("2026-11-23T12:00:00Z");
        setTime(start);
        BackupTask.pauseBackupsFor(1_000L);

        setTime(start.plusSeconds(1));
        Assert.assertFalse(BackupTask.resumeBackups());
    }

    @Test(expected = ArithmeticException.class)
    public void pauseFor_whenEndTimeOverflows_ShouldThrow() {
        BackupTask.pauseBackupsFor(Long.MAX_VALUE);
    }

    @Test
    public void lastPlayerLogout_ShouldEndTimedPause() {
        EntityPlayerMP leaving = addPlayer();
        BackupTask.pauseBackupsFor(3_600_000L);

        BackupTask.onPlayerLoggedOut(server, leaving);
        Assert.assertFalse(BackupTask.isPaused());
    }

    @Test
    public void logout_whenOthersOnline_ShouldStayPaused() {
        EntityPlayerMP leaving = addPlayer();
        addPlayer();
        BackupTask.pauseBackups();

        BackupTask.onPlayerLoggedOut(server, leaving);
        Assert.assertTrue(BackupTask.isPaused());
    }
}
