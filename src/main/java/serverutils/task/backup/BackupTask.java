package serverutils.task.backup;

import static serverutils.ServerUtilitiesConfig.backups;
import static serverutils.ServerUtilitiesNotifications.BACKUP_START;
import static serverutils.lib.util.FileUtils.SizeUnit;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.ServerUtilitiesNotifications;
import serverutils.data.ClaimedChunks;
import serverutils.lib.data.Universe;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.FileUtils;
import serverutils.lib.util.ServerUtils;
import serverutils.task.Task;

public class BackupTask extends Task {

    public static final Pattern BACKUP_NAME_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}(.*)");
    public static final File BACKUP_TEMP_FOLDER = new File("serverutilities/temp/");
    public static File backupsFolder;
    public static ThreadBackup thread;
    public static boolean hadPlayer = false;
    private ICommandSender sender;
    private String customName = "";
    private boolean post = false;

    static {
        backupsFolder = backups.backup_folder_path.isEmpty() ? new File("/backups/")
                : new File(backups.backup_folder_path);
        if (!backupsFolder.exists()) backupsFolder.mkdirs();
        clearOldBackups();
        ServerUtilities.LOGGER.info("Backups folder - {}", backupsFolder.getAbsolutePath());
    }

    public BackupTask() {
        super(Ticks.HOUR.x(backups.backup_timer));
    }

    public BackupTask(@Nullable ICommandSender ics, String customName) {
        this.customName = customName;
        this.sender = ics;
    }

    public BackupTask(boolean postCleanup) {
        super(0);
        this.post = postCleanup;
    }

    @Override
    public boolean isRepeatable() {
        return !post;
    }

    @Override
    public void execute(Universe universe) {
        if (post) {
            postBackup(universe);
            return;
        }
        if (thread != null) return;
        boolean auto = sender == null;

        if (auto && !backups.enable_backups) return;

        MinecraftServer server = universe.server;
        if (auto && backups.need_online_players) {
            if (!hasOnlinePlayers(server) && !hadPlayer) return;
            hadPlayer = false;
        }
        ServerUtilitiesNotifications.backupNotification(BACKUP_START, "cmd.backup_start");

        try {
            for (int i = 0; i < server.worldServers.length; ++i) {
                if (server.worldServers[i] != null) {
                    WorldServer worldserver = server.worldServers[i];
                    worldserver.levelSaving = true;
                    worldserver.saveAllChunks(true, null);
                }
            }
        } catch (Exception ex) {
            ServerUtilities.LOGGER.info("An error occurred while turning off auto-save.", ex);
        }

        File worldDir = DimensionManager.getCurrentSaveRootDirectory();

        Set<ChunkDimPos> backupChunks = new HashSet<>();
        if (backups.only_backup_claimed_chunks && ClaimedChunks.isActive()) {
            backupChunks.addAll(ClaimedChunks.instance.getAllClaimedPositions());
            BACKUP_TEMP_FOLDER.mkdirs();
        }

        if (backups.use_separate_thread) {
            thread = new ThreadBackup(worldDir, customName, backupChunks);
            thread.start();
        } else {
            ThreadBackup.doBackup(worldDir, customName, backupChunks);
        }
        universe.scheduleTask(new BackupTask(true));
    }

    public static void clearOldBackups() {
        File[] files = backupsFolder.listFiles();
        if (files == null || files.length == 0) return;

        List<File> backupFiles = Arrays.stream(files).filter(
                file -> backups.delete_custom_name_backups || BACKUP_NAME_PATTERN.matcher(file.getName()).matches())
                .sorted(Comparator.comparingLong(File::lastModified)).collect(Collectors.toList());

        int maxGb = backups.max_folder_size;
        if (maxGb > 0) {
            long currentSize = backupFiles.stream().mapToLong(file -> FileUtils.getSize(file, SizeUnit.GB)).sum();
            if (currentSize <= maxGb) return;
            deleteOldBackups(backupFiles, currentSize, maxGb);

        } else if (backupFiles.size() > backups.backups_to_keep) {
            deleteExcessBackups(backupFiles);
        }
    }

    private static void deleteOldBackups(List<File> backupFiles, long currentSize, int maxGb) {
        int deleted = 0;
        for (File file : backupFiles) {
            if (currentSize <= maxGb) break;
            currentSize -= FileUtils.getSize(file, SizeUnit.GB);
            ServerUtilities.LOGGER.info("Deleting old backup: {}", file.getPath());
            FileUtils.delete(file);
            deleted++;
        }
        ServerUtilities.LOGGER.info("Deleted {} old backups", deleted);
    }

    private static void deleteExcessBackups(List<File> backupFiles) {
        int toDelete = backupFiles.size() - ServerUtilitiesConfig.backups.backups_to_keep;
        ServerUtilities.LOGGER.info("Deleting {} old backups", toDelete);
        for (int i = 0; i < toDelete; i++) {
            File file = backupFiles.get(i);
            ServerUtilities.LOGGER.info("Deleted old backup: {}", file.getPath());
            FileUtils.delete(file);
        }
    }

    private boolean hasOnlinePlayers(MinecraftServer server) {
        return !server.getConfigurationManager().playerEntityList.isEmpty();
    }

    private void postBackup(Universe universe) {
        if (thread != null && !thread.isDone) {
            setNextTime(System.currentTimeMillis() + Ticks.SECOND.millis());
            universe.scheduleTask(this);
            return;
        }

        clearOldBackups();
        FileUtils.delete(BACKUP_TEMP_FOLDER);

        thread = null;
        try {
            MinecraftServer server = ServerUtils.getServer();

            for (int i = 0; i < server.worldServers.length; ++i) {
                if (server.worldServers[i] != null) {
                    WorldServer worldserver = server.worldServers[i];

                    if (worldserver.levelSaving) {
                        worldserver.levelSaving = false;
                    }
                }
            }
        } catch (Exception ex) {
            ServerUtilities.LOGGER.info("An error occurred while turning on auto-save.", ex);
        }
    }
}
