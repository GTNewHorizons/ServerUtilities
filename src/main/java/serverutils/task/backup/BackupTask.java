package serverutils.task.backup;

import static serverutils.ServerUtilitiesConfig.backups;
import static serverutils.ServerUtilitiesNotifications.BACKUP;
import static serverutils.lib.util.FileUtils.SizeUnit;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.ThreadedFileIOBase;
import net.minecraftforge.common.DimensionManager;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.data.ClaimedChunks;
import serverutils.lib.OtherMods;
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
    private static final Map<WorldServer, Boolean> worldSaveStates = new IdentityHashMap<>();
    public static ThreadBackup thread;
    public static boolean hadPlayer = false;
    private ICommandSender sender;
    private String customName = "";
    private boolean post = false;
    private boolean forceOnlyClaimed = false;

    static {
        BACKUP_FOLDER = backups.backup_folder_path.isEmpty() ? new File("/backups/")
                : new File(backups.backup_folder_path);
        if (!BACKUP_FOLDER.exists()) BACKUP_FOLDER.mkdirs();
        // Class initialization runs before any backup worker can own an archive staging file.
        deleteAbandonedArchives(BACKUP_FOLDER);
        clearOldBackups();
        ServerUtilities.LOGGER.info("Backups folder - {}", BACKUP_FOLDER.getAbsolutePath());
    }

    public BackupTask() {
        super(Ticks.HOUR.x(backups.backup_timer));
    }

    public BackupTask(@Nullable ICommandSender ics, String customName, final boolean forceOnlyClaimed) {
        this(ics, customName);
        this.forceOnlyClaimed = forceOnlyClaimed;
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
        if (isBackupRunning()) return;
        if (!worldSaveStates.isEmpty()) postBackup(universe);
        boolean auto = sender == null;

        if (auto && !backups.enable_backups) return;

        MinecraftServer server = universe.server;
        if (auto && backups.need_online_players) {
            if (!hasOnlinePlayers(server) && !hadPlayer) return;
            hadPlayer = false;
        }

        long started = System.nanoTime();
        long phase = started;
        ServerUtilities.LOGGER.info("Backup preparation started on server thread");
        boolean backupStarted = false;
        boolean snapshotPrepared = false;
        try {
            // Must run before saveAllChunks so level.dat is written with the current host inventory, otherwise
            // the single-player host's inventory in the backup is stale and items can dupe/vanish on restore.
            server.getConfigurationManager().saveAllPlayerData();
            phase = logBackupPhase("player save", phase);
            saveAndDisableWorldSaving(server.worldServers);
            phase = logBackupPhase("world save", phase);

            flushChunkSaves(server.worldServers);
            phase = logBackupPhase("chunk flush", phase);

            if (!backups.silent_backup) {
                BACKUP.sendAll(StringUtils.color("cmd.backup_start", EnumChatFormatting.LIGHT_PURPLE));
            }
            Set<ChunkDimPos> backupChunks = new HashSet<>();
            boolean onlyClaimed = this.forceOnlyClaimed || backups.only_backup_claimed_chunks;
            if (onlyClaimed) {
                if (!ClaimedChunks.isActive()) throw new IllegalStateException("Chunk claiming is not active");
                ClaimedChunks.instance.processQueue();
                backupChunks.addAll(ClaimedChunks.instance.getAllClaimedPositions());
                // noinspection ResultOfMethodCallIgnored
                BACKUP_TEMP_FOLDER.mkdirs();
            }

            phase = logBackupPhase("backup notification and claims", phase);
            universe.saveForBackup();
            phase = logBackupPhase("SU data save", phase);
            drainQueuedWrites();
            phase = logBackupPhase("queued world data flush", phase);
            File worldDir = DimensionManager.getCurrentSaveRootDirectory();
            ICompress compressor = ICompress.createCompressor();
            universe.scheduleTask(new BackupTask(true));
            if (backups.use_separate_thread) {
                phase = logBackupPhase("backup setup", phase);
                ThreadBackup.Snapshot snapshot = ThreadBackup.snapshotFiles(worldDir);
                logBackupPhase("file snapshot", phase);
                snapshotPrepared = true;
                thread = new ThreadBackup(compressor, worldDir, customName, backupChunks, snapshot, onlyClaimed);
                thread.start();
            } else {
                ThreadBackup.doBackup(compressor, worldDir, customName, backupChunks, null, onlyClaimed);
            }
            backupStarted = true;
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            ServerUtils.notifyChat(
                    server,
                    null,
                    new ChatComponentText(
                            EnumChatFormatting.RED + "An error occurred while preparing backup. " + ex.getMessage()));
            ServerUtilities.LOGGER.info("An error occurred while preparing backup, Aborting!", ex);
        } finally {
            if (!backupStarted) {
                restoreWorldSaving();
                if (snapshotPrepared) ThreadBackup.deleteSnapshot();
            }
            logBackupPhase("total server-thread backup task (started=" + backupStarted + ")", started);
        }
    }

    private static long logBackupPhase(String phase, long started) {
        long finished = System.nanoTime();
        ServerUtilities.LOGGER.info("Backup timing: {} took {} ms", phase, (finished - started) / 1_000_000L);
        return System.nanoTime();
    }

    /** Reports retained Hodgepodge failures without requiring that mod, or its newer flush API. */
    static void drainQueuedWrites() throws Exception {
        ThreadedFileIOBase.threadedIOInstance.waitForFinish();
        if (!OtherMods.isHodgepodgeLoaded()) return;
        Class<?> tweaks;
        try {
            tweaks = Class.forName("com.mitchej123.hodgepodge.config.TweaksConfig");
        } catch (ClassNotFoundException e) {
            return;
        }
        try {
            if (!tweaks.getField("threadedWorldDataSaving").getBoolean(null)) return;
        } catch (NoSuchFieldException e) {
            return;
        }
        flushWorldDataSaver(Class.forName("com.mitchej123.hodgepodge.util.WorldDataSaver"));
    }

    static void flushWorldDataSaver(Class<?> saver) throws Exception {
        Method flush;
        try {
            flush = saver.getMethod("flush");
        } catch (NoSuchMethodException e) {
            ServerUtilities.LOGGER.warn(
                    "Hodgepodge WorldDataSaver has no flush(); queued writes drained, but write failures cannot be confirmed");
            return;
        }
        try {
            flush.invoke(saver.getField("INSTANCE").get(null));
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new IllegalStateException(cause);
        }
    }

    static void flushChunkSaves(WorldServer[] worlds) throws InterruptedException {
        // Let the worker finish first: concurrent writers can commit older chunk data last.
        ThreadedFileIOBase.threadedIOInstance.waitForFinish();
        for (WorldServer world : worlds) {
            if (world == null) continue;
            boolean suspended = world.levelSaving;
            try {
                world.levelSaving = false;
                // Vanilla can remove a loader just after new work is submitted to it. Drain that stranded work too.
                world.saveChunkData();
            } finally {
                world.levelSaving = suspended;
            }
        }
        ThreadedFileIOBase.threadedIOInstance.waitForFinish();
    }

    static void saveAndDisableWorldSaving(WorldServer[] worlds) throws MinecraftException {
        try {
            for (WorldServer world : worlds) {
                if (world == null) continue;
                worldSaveStates.putIfAbsent(world, world.levelSaving);
                world.levelSaving = false;
                world.saveAllChunks(true, null);
                world.levelSaving = true;
            }
            // A suspended world logs its usual "Saving chunks for level" line and then saves nothing, so record the
            // suspension so missing state restoration can be diagnosed from the log.
            ServerUtilities.LOGGER.info("Suspended world saving in {} dimensions for backup", worldSaveStates.size());
        } catch (MinecraftException | RuntimeException ex) {
            restoreWorldSaving();
            throw ex;
        }
    }

    static void restoreWorldSaving() {
        if (!worldSaveStates.isEmpty()) {
            ServerUtilities.LOGGER.info(
                    "Restored previous world-saving states in {} dimensions after backup",
                    worldSaveStates.size());
        }
        worldSaveStates.forEach((world, levelSaving) -> world.levelSaving = levelSaving);
        worldSaveStates.clear();
    }

    public static boolean isWorldSavingSuspended() {
        return !worldSaveStates.isEmpty();
    }

    public static void suspendNewWorldSaving(WorldServer world) {
        if (isWorldSavingSuspended()) {
            if (worldSaveStates.putIfAbsent(world, world.levelSaving) == null) {
                ServerUtilities.LOGGER.info(
                        "Dimension {} loaded during a backup; world saving suspended there until it finishes",
                        world.provider.dimensionId);
            }
            world.levelSaving = true;
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

    public static void clearOldBackups() {
        File[] files = BACKUP_FOLDER.listFiles();
        if (files == null || files.length == 0) return;

        List<File> backupFiles = Arrays.stream(files)
                // Only startup reclaims staging files: a running worker may still own one here.
                .filter(file -> !isArchiveStagingFile(file))
                .filter(
                        file -> backups.delete_custom_name_backups
                                || BACKUP_NAME_PATTERN.matcher(file.getName()).matches())
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

    public static boolean isBackupRunning() {
        return thread != null && thread.isAlive();
    }

    public static void stopBackupThread() {
        ThreadBackup backupThread = thread;
        boolean interrupted = false;
        if (backupThread != null) {
            backupThread.interrupt();
            while (backupThread.isAlive()) {
                try {
                    backupThread.join();
                } catch (InterruptedException ex) {
                    interrupted = true;
                    backupThread.interrupt();
                }
            }
        }
        thread = null;
        restoreWorldSaving();
        if (interrupted) Thread.currentThread().interrupt();
    }

    private boolean hasOnlinePlayers(MinecraftServer server) {
        return !server.getConfigurationManager().playerEntityList.isEmpty();
    }

    private void postBackup(Universe universe) {
        if (worldSaveStates.isEmpty()) return;
        if (isBackupRunning()) {
            setNextTime(System.currentTimeMillis() + Ticks.SECOND.millis());
            universe.scheduleTask(this);
            return;
        }

        thread = null;
        restoreWorldSaving();
        clearOldBackups();
        FileUtils.delete(BACKUP_TEMP_FOLDER);
    }
}
