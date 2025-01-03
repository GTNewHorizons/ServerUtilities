package serverutils.task.backup;

import static serverutils.ServerUtilitiesConfig.backups;
import static serverutils.ServerUtilitiesNotifications.BACKUP;
import static serverutils.lib.util.FileUtils.SizeUnit;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import it.unimi.dsi.fastutil.ints.Int2BooleanArrayMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.data.ClaimedChunks;
import serverutils.lib.data.Universe;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.FileUtils;
import serverutils.lib.util.ServerUtils;
import serverutils.lib.util.StringUtils;
import serverutils.lib.util.compression.ICompress;
import serverutils.task.Task;

public class BackupTask extends Task {

    public static final Pattern BACKUP_NAME_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}(.*)");
    public static final File BACKUP_TEMP_FOLDER = new File("serverutilities/temp/");
    public static final File BACKUP_FOLDER;
    private static final Int2BooleanMap dimSaveStates = new Int2BooleanArrayMap();
    public static ThreadBackup thread;
    public static boolean hadPlayer = false;
    private ICommandSender sender;
    private String customName = "";
    private boolean post = false;

    static {
        BACKUP_FOLDER = backups.backup_folder_path.isEmpty() ? new File("/backups/")
                : new File(backups.backup_folder_path);
        if (!BACKUP_FOLDER.exists()) BACKUP_FOLDER.mkdirs();
        clearOldBackups();
        ServerUtilities.LOGGER.info("Backups folder - {}", BACKUP_FOLDER.getAbsolutePath());
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

        dimSaveStates.clear();
        try {
            for (int i = 0; i < server.worldServers.length; ++i) {
                WorldServer world = server.worldServers[i];
                if (world != null) {
                    dimSaveStates.put(i, world.levelSaving);
                    world.saveAllChunks(true, null);
                    world.levelSaving = true;
                }
            }
        } catch (Exception ex) {
            ServerUtils.notifyChat(
                    server,
                    null,
                    new ChatComponentText(
                            EnumChatFormatting.RED + "An error occurred while preparing backup. " + ex.getMessage()));
            ServerUtilities.LOGGER.info("An error occurred while preparing backup, Aborting!", ex);
            return;
        }

        server.getConfigurationManager().saveAllPlayerData();
        BACKUP.sendAll(StringUtils.color("cmd.backup_start", EnumChatFormatting.LIGHT_PURPLE));
        Set<ChunkDimPos> backupChunks = new HashSet<>();
        if (backups.only_backup_claimed_chunks && ClaimedChunks.isActive()) {
            backupChunks.addAll(ClaimedChunks.instance.getAllClaimedPositions());
            // noinspection ResultOfMethodCallIgnored
            BACKUP_TEMP_FOLDER.mkdirs();
        }

        File worldDir = DimensionManager.getCurrentSaveRootDirectory();
        ICompress compressor = ICompress.createCompressor();
        if (backups.use_separate_thread) {
            thread = new ThreadBackup(compressor, worldDir, customName, backupChunks);
            thread.start();
        } else {
            ThreadBackup.doBackup(compressor, worldDir, customName, backupChunks);
        }
        universe.scheduleTask(new BackupTask(true));
    }

    public static void clearOldBackups() {
        File[] files = BACKUP_FOLDER.listFiles();
        if (files == null || files.length == 0) return;

        List<File> backupFiles = Arrays.stream(files).filter(
                file -> backups.delete_custom_name_backups || BACKUP_NAME_PATTERN.matcher(file.getName()).matches())
                .sorted(Comparator.comparingLong(File::lastModified)).collect(Collectors.toList());

        long maxSize = backups.max_folder_size * SizeUnit.GB.getSize();
        if (maxSize > 0) {
            long currentSize = backupFiles.stream().mapToLong(FileUtils::getSize).sum();
            if (currentSize <= maxSize) return;
            deleteOldBackups(backupFiles, currentSize, maxSize);

        } else if (backupFiles.size() > backups.backups_to_keep) {
            deleteExcessBackups(backupFiles);
        }
    }

    private static void deleteOldBackups(List<File> backupFiles, long currentSize, long maxSize) {
        int deleted = 0;
        for (File file : backupFiles) {
            if (currentSize <= maxSize) break;
            currentSize -= FileUtils.getSize(file);
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
                WorldServer world = server.worldServers[i];
                if (world != null) {
                    if (dimSaveStates.containsKey(i)) {
                        world.levelSaving = dimSaveStates.get(i);
                    } else {
                        world.levelSaving = false;
                    }

                }
            }
        } catch (Exception ex) {
            ServerUtilities.LOGGER.info("An error occurred while turning on auto-save.", ex);
        }
    }
}
