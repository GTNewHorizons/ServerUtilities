package serverutils.task.backup;

import static serverutils.ServerUtilitiesConfig.backups;

import java.time.Clock;
import java.time.Instant;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import serverutils.ServerUtilities;
import serverutils.lib.data.Universe;
import serverutils.lib.math.Ticks;
import serverutils.task.Task;

public class BackupTask extends Task {

    private static Instant lastPlayerActivity = Instant.MIN;
    private static volatile Instant pausedUntil = null;
    static Clock clock = Clock.systemUTC();

    public BackupTask() {
        super(Ticks.getFromMillis(BackupDuration.parse(BackupDuration.normalizeTimer(backups.backup_timer))));
        if (getNextTime() < 0) throw new IllegalArgumentException("backup_timer exceeds scheduling range");
    }

    @Override
    public void execute(Universe universe) {
        if (!backups.enable_backups) return;

        if (isPaused()) {
            ServerUtilities.LOGGER.debug("Backup skipped, because it's currently paused.");
            return;
        }

        MinecraftServer server = universe.server;
        if (hasOnlinePlayers(server)) lastPlayerActivity = Instant.now();

        if (backups.need_online_players && !lastPlayerActivity.isAfter(BackupRoutine.getLastSuccessTime())) {
            return;
        }

        BackupRoutine.Options options = BackupRoutine.Options.fromConfig();

        BackupRoutine routine = new BackupRoutine(universe, options);

        if (!BackupCoordinator.startBackup(routine)) {
            if (BackupCoordinator.hasAbandonedBackup()) {
                // Can last until restart if the worker is stuck, so this must not go unnoticed
                ServerUtilities.LOGGER.warn(
                        "Did not execute scheduled backup: a backup stopped earlier is still running and blocks new ones until it finishes. Check the backup storage if this persists.");
            } else {
                ServerUtilities.LOGGER.info("Did not execute scheduled backup, another backup was in progress.");
            }
        }
    }

    public static void newPlayerActivity() {
        lastPlayerActivity = Instant.now();
    }

    private boolean hasOnlinePlayers(MinecraftServer server) {
        return !server.getConfigurationManager().playerEntityList.isEmpty();
    }

    public static boolean pauseBackups() {
        if (Instant.MAX.equals(pausedUntil)) {
            return false;
        }
        pausedUntil = Instant.MAX;
        return true;
    }

    public static Instant pauseBackupsFor(long millis) {
        pausedUntil = Instant.ofEpochMilli(Math.addExact(clock.millis(), millis));
        return pausedUntil;
    }

    public static boolean resumeBackups() {
        if (!isPaused()) {
            return false;
        }
        pausedUntil = null;
        return true;
    }

    public static boolean isPaused() {
        if (pausedUntil != null && !clock.instant().isBefore(pausedUntil)) {
            pausedUntil = null;
            ServerUtilities.LOGGER.info("Automatic backups resumed, the pause expired.");
        }
        return pausedUntil != null;
    }

    public static void onPlayerLoggedOut(MinecraftServer server, EntityPlayerMP leavingPlayer) {
        if (isPaused() && isLastPlayer(server, leavingPlayer)) {
            resumeBackups();
            ServerUtilities.LOGGER.info("The last player logged out, resuming automatic backups");
        }
    }

    private static boolean isLastPlayer(MinecraftServer server, EntityPlayerMP leavingPlayer) {
        for (Object player : server.getConfigurationManager().playerEntityList) {
            if (player != leavingPlayer) {
                return false;
            }
        }
        return true;
    }
}
