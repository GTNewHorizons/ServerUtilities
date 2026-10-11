package serverutils.task.backup;

import static serverutils.ServerUtilitiesConfig.backups;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.time.Instant;
import java.util.regex.Pattern;

import net.minecraft.server.MinecraftServer;

import serverutils.ServerUtilities;
import serverutils.lib.data.Universe;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.FileUtils;
import serverutils.lib.util.compression.ICompress;
import serverutils.task.Task;

public class BackupTask extends Task {

    public static final Pattern BACKUP_NAME_PATTERN = BackupRetention.LEGACY_NAME;
    public static final File BACKUP_TEMP_FOLDER = new File("serverutilities/temp/");
    public static final File BACKUP_FOLDER;
    static volatile Thread retentionThread;
    public static volatile ThreadBackup thread;
    private boolean post = false;
    private boolean forceOnlyClaimed = false;
    private boolean overwrite;

    public static Instant lastPlayerActivity = Instant.MIN;

    static {
        BACKUP_FOLDER = backups.backup_folder_path.isEmpty() ? new File("/backups/")
                : new File(backups.backup_folder_path);
        FileUtils.ensureExists(BACKUP_FOLDER);
        // Class initialization runs before any backup worker can own an archive staging file.
        deleteAbandonedArchives(BACKUP_FOLDER);
        ServerUtilities.LOGGER.info("Backups folder - {}", BACKUP_FOLDER.getAbsolutePath());
    }

    public BackupTask() {
        super(Ticks.getFromMillis(BackupDuration.parse(BackupDuration.normalizeTimer(backups.backup_timer))));
        if (getNextTime() < 0) throw new IllegalArgumentException("backup_timer exceeds scheduling range");
    }

    @Override
    public boolean isRepeatable() {
        return !post;
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

        ICompress compressor = ICompress.createCompressor();
        BackupRoutine routine = new BackupRoutine(universe, compressor, options);
        boolean isStarted = BackupCoordinator.startBackup(routine);

//        } catch (Exception ex) {
//            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
//            ServerUtils.notifyChat(
//                    server,
//                    null,
//                    new ChatComponentText(
//                            EnumChatFormatting.RED + "An error occurred while preparing backup. " + ex.getMessage()));
//            ServerUtilities.LOGGER.info("An error occurred while preparing backup, Aborting!", ex);
//        } finally {
//            if (!backupStarted) {
//                finishPlayerActivity(false);
//                restoreWorldSaving();
//                if (snapshotPrepared) ThreadBackup.deleteSnapshot();
//            } else if (thread == null) finishPlayerActivity(backupSucceeded);
//            backupPreparing = false;
//            ServerUtilities.LOGGER.info(
//                    "Backup server-thread timing: started={}; total={} ms; {}",
//                    backupStarted,
//                    (System.nanoTime() - started) / 1_000_000L,
//                    timings);
//        }
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
