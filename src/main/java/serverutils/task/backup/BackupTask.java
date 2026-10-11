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
import serverutils.lib.util.compression.ICompress;
import serverutils.task.Task;

public class BackupTask extends Task {

    public static Instant lastPlayerActivity = Instant.MIN;
    /** Only touched from the server thread's task loop. */
    private static boolean retryPending;

    static {
        // Class initialization runs before any backup worker can own an archive staging file.
        deleteAbandonedArchives(BackupRoutine.BACKUP_FOLDER);
    }

    public BackupTask() {
        this(null);
    }

    /** A null interval is the configured repeating timer; any other interval is a single delayed retry. */
    public BackupTask(Ticks interval) {
        super(
                interval == null
                        ? Ticks.getFromMillis(BackupDuration.parse(BackupDuration.normalizeTimer(backups.backup_timer)))
                        : interval);
        if (getNextTime() < 0) throw new IllegalArgumentException("backup_timer exceeds scheduling range");
        repeatable = interval == null;
    }

    @Override
    public void execute(Universe universe) {
        if (!repeatable) retryPending = false;
        if (!backups.enable_backups) return;

        MinecraftServer server = universe.server;
        if (hasOnlinePlayers(server)) lastPlayerActivity = Instant.now();

        if (backups.need_online_players && !lastPlayerActivity.isAfter(BackupRoutine.getLastSuccessTime())) {
            return;
        }

        BackupRoutine.Options options = BackupRoutine.Options.fromConfig();

        ICompress compressor = ICompress.createCompressor();
        BackupRoutine routine = new BackupRoutine(universe, compressor, options);
        boolean isStarted = BackupCoordinator.startBackup(routine);

        // Busy with another backup or a retention scan; try once more shortly instead of waiting a whole interval.
        if (!isStarted && !retryPending) {
            retryPending = true;
            universe.scheduleTask(new BackupTask(Ticks.SECOND));
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
