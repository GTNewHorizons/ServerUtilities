package serverutils.task.backup;

import static serverutils.ServerUtilitiesConfig.backups;

import java.time.Instant;

import net.minecraft.server.MinecraftServer;

import serverutils.ServerUtilities;
import serverutils.lib.data.Universe;
import serverutils.lib.math.Ticks;
import serverutils.task.Task;

public class BackupTask extends Task {

    private static Instant lastPlayerActivity = Instant.MIN;

    public BackupTask() {
        super(Ticks.getFromMillis(BackupDuration.parse(BackupDuration.normalizeTimer(backups.backup_timer))));
        if (getNextTime() < 0) throw new IllegalArgumentException("backup_timer exceeds scheduling range");
    }

    @Override
    public void execute(Universe universe) {
        if (!backups.enable_backups) return;

        MinecraftServer server = universe.server;
        if (hasOnlinePlayers(server)) lastPlayerActivity = Instant.now();

        if (backups.need_online_players && !lastPlayerActivity.isAfter(BackupRoutine.getLastSuccessTime())) {
            return;
        }

        BackupRoutine.Options options = BackupRoutine.Options.fromConfig();

        BackupRoutine routine = new BackupRoutine(universe, options);

        if (!BackupCoordinator.startBackup(routine)) {
            ServerUtilities.LOGGER.info("Did not execute scheduled backup, another backup was in progress.");
        }
    }

    public static void newPlayerActivity() {
        lastPlayerActivity = Instant.now();
    }

    private boolean hasOnlinePlayers(MinecraftServer server) {
        return !server.getConfigurationManager().playerEntityList.isEmpty();
    }
}
