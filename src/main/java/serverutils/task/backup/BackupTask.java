package serverutils.task.backup;

import static serverutils.ServerUtilitiesConfig.backups;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.time.Instant;

import net.minecraft.server.MinecraftServer;

import serverutils.ServerUtilities;
import serverutils.lib.data.Universe;
import serverutils.lib.math.Ticks;
import serverutils.task.Task;

public class BackupTask extends Task {

    private static Instant lastPlayerActivity = Instant.MIN;

    static {
        // Class initialization runs before any backup worker can own an archive staging file.
        deleteAbandonedArchives(BackupRoutine.BACKUP_FOLDER);
    }

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

    static void deleteAbandonedArchives(File folder) {
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (!isArchiveStagingFile(file) || !Files.isRegularFile(file.toPath(), LinkOption.NOFOLLOW_LINKS)) continue;
            try {
                Files.delete(file.toPath());
            } catch (IOException ex) {
                ServerUtilities.LOGGER.warn("Could not delete abandoned backup staging file {}", file, ex);
            }
        }
    }

    private static boolean isArchiveStagingFile(File file) {
        return file.getName().startsWith(".su-save-") && file.getName().endsWith(".tmp");
    }

    public static void newPlayerActivity() {
        lastPlayerActivity = Instant.now();
    }

    private boolean hasOnlinePlayers(MinecraftServer server) {
        return !server.getConfigurationManager().playerEntityList.isEmpty();
    }
}
