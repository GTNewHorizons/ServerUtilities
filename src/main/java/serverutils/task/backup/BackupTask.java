package serverutils.task.backup;

import static serverutils.ServerUtilitiesConfig.backups;
import static serverutils.ServerUtilitiesNotifications.BACKUP;
import static serverutils.lib.util.FileUtils.SizeUnit;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.regex.Pattern;

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

    public static final Pattern BACKUP_NAME_PATTERN = BackupRetention.LEGACY_NAME;
    public static final File BACKUP_TEMP_FOLDER = new File("serverutilities/temp/");
    public static final File BACKUP_FOLDER;
    private static final Map<WorldServer, Boolean> worldSaveStates = new IdentityHashMap<>();
    private static boolean backupSucceeded;
    private static volatile boolean backupPreparing;
    private static volatile boolean stopping;
    private static volatile boolean prunePending;
    static volatile Thread retentionThread;
    public static volatile ThreadBackup thread;
    public static boolean hadPlayer = false;
    private ICommandSender sender;
    private String customName = "";
    private boolean post = false;
    private boolean forceOnlyClaimed = false;
    private boolean overwrite;
    private boolean deferred;
    private boolean started;

    static {
        BACKUP_FOLDER = backups.backup_folder_path.isEmpty() ? new File("/backups/")
                : new File(backups.backup_folder_path);
        if (!BACKUP_FOLDER.exists()) BACKUP_FOLDER.mkdirs();
        // Class initialization runs before any backup worker can own an archive staging file.
        deleteAbandonedArchives(BACKUP_FOLDER);
        ServerUtilities.LOGGER.info("Backups folder - {}", BACKUP_FOLDER.getAbsolutePath());
    }

    public BackupTask() {
        super(Ticks.getFromMillis(BackupDuration.parse(BackupDuration.normalizeTimer(backups.backup_timer))));
        if (getNextTime() < 0) throw new IllegalArgumentException("backup_timer exceeds scheduling range");
    }

    public BackupTask(@Nullable ICommandSender ics, String customName, final boolean forceOnlyClaimed) {
        this(ics, customName);
        this.forceOnlyClaimed = forceOnlyClaimed;
    }

    public BackupTask(@Nullable ICommandSender ics, String customName, boolean forceOnlyClaimed, boolean overwrite) {
        this(ics, customName, forceOnlyClaimed);
        this.overwrite = overwrite;
    }

    public boolean hasStarted() {
        return started;
    }

    public boolean isDeferred() {
        return deferred;
    }

    @Override
    public long getInterval() {
        return deferred && sender == null ? Ticks.SECOND.millis() : super.getInterval();
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
        started = false;
        deferred = backupPreparing || isBackupRunning();
        if (deferred) return;
        if (!worldSaveStates.isEmpty()) postBackup(universe);
        boolean auto = sender == null;

        if (auto && !backups.enable_backups) return;

        MinecraftServer server = universe.server;
        if (auto && backups.need_online_players) {
            if (!hasOnlinePlayers(server) && !hadPlayer) return;
            hadPlayer = false;
        }

        synchronized (BackupTask.class) {
            if (backupPreparing || isBackupRunning()) {
                deferred = true;
                return;
            }
            backupPreparing = true;
        }
        backupSucceeded = false;
        long createdAt = System.currentTimeMillis();

        long started = System.nanoTime();
        long phase = started;
        StringBuilder timings = new StringBuilder();
        boolean backupStarted = false;
        boolean snapshotPrepared = false;
        try {
            ThreadBackup.validateBackupName(customName);
            if (!customName.isEmpty() || overwrite) ThreadBackup.backupDestination(customName, overwrite);
            // Must run before saveAllChunks so level.dat is written with the current host inventory, otherwise
            // the single-player host's inventory in the backup is stale and items can dupe/vanish on restore.
            server.getConfigurationManager().saveAllPlayerData();
            phase = recordBackupPhase(timings, "player save", phase);
            saveAndDisableWorldSaving(server.worldServers);
            phase = recordBackupPhase(timings, "world save", phase);

            flushChunkSaves(server.worldServers);
            phase = recordBackupPhase(timings, "chunk flush", phase);

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

            phase = recordBackupPhase(timings, "notification and claims", phase);
            UUID worldId = universe.getUUID();
            String backupWorldId = worldId == null ? null : worldId.toString();
            universe.saveForBackup();
            phase = recordBackupPhase(timings, "SU data save", phase);
            drainQueuedWrites();
            phase = recordBackupPhase(timings, "queued data flush", phase);
            File worldDir = DimensionManager.getCurrentSaveRootDirectory();
            ICompress compressor = ICompress.createCompressor();
            universe.scheduleTask(new BackupTask(true));
            if (backups.use_separate_thread) {
                phase = recordBackupPhase(timings, "setup", phase);
                // Without a snapshot the worker lists and reads the live files itself, so the server thread does no
                // file I/O here, at the cost of files changing while the archive is written.
                ThreadBackup.Snapshot snapshot = null;
                if (!backups.prefer_speed_over_backup_consistency) {
                    snapshot = ThreadBackup.snapshotFiles(worldDir);
                    recordBackupPhase(timings, "file snapshot", phase);
                    snapshotPrepared = true;
                }
                thread = new ThreadBackup(
                        compressor,
                        worldDir,
                        customName,
                        backupChunks,
                        snapshot,
                        onlyClaimed,
                        backupWorldId,
                        createdAt,
                        overwrite);
                thread.start();
                backupStarted = true;
            } else {
                phase = recordBackupPhase(timings, "setup", phase);
                backupSucceeded = ThreadBackup.doBackup(
                        compressor,
                        worldDir,
                        customName,
                        backupChunks,
                        null,
                        onlyClaimed,
                        backupWorldId,
                        createdAt,
                        overwrite);
                recordBackupPhase(timings, "archive", phase);
                backupStarted = backupSucceeded;
            }
            this.started = backupStarted;
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
            backupPreparing = false;
            ServerUtilities.LOGGER.info(
                    "Backup server-thread timing: started={}; total={} ms; {}",
                    backupStarted,
                    (System.nanoTime() - started) / 1_000_000L,
                    timings);
        }
    }

    private static long recordBackupPhase(StringBuilder timings, String phase, long started) {
        long finished = System.nanoTime();
        if (timings.length() > 0) timings.append("; ");
        timings.append(phase).append('=').append((finished - started) / 1_000_000L).append(" ms");
        return finished;
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

    public static synchronized void clearOldBackups() {
        prunePending = true;
        if (hasRetentionPolicy()) {
            String[] policy = backups.retention_policy.clone();
            boolean deleteCustom = backups.delete_custom_name_backups;
            long maxSize = backups.max_folder_size * SizeUnit.GB.getSize();
            if (startRetentionTask(() -> pruneRetention(policy, deleteCustom, maxSize))) prunePending = false;
            return;
        }
        int keepCount = backups.backups_to_keep;
        long maxSize = backups.max_folder_size * SizeUnit.GB.getSize();
        boolean deleteCustom = backups.delete_custom_name_backups;
        if (startRetentionTask(() -> pruneLegacy(keepCount, maxSize, deleteCustom))) prunePending = false;
    }

    private static void pruneLegacy(int keepCount, long maxSize, boolean deleteCustom) {
        if (maxSize < 0) {
            ServerUtilities.LOGGER.warn("Skipping legacy backup pruning: negative size limit");
            return;
        }
        try {
            List<BackupRetention.Archive> backupFiles = readLegacyBackups(BACKUP_FOLDER, deleteCustom);
            if (hasRetentionPolicy() || keepCount != backups.backups_to_keep
                    || deleteCustom != backups.delete_custom_name_backups
                    || maxSize != backups.max_folder_size * SizeUnit.GB.getSize()) {
                ServerUtilities.LOGGER.info("Retention settings changed; skipping stale legacy pruning plan");
                return;
            }
            clearLegacyBackups(backupFiles, keepCount, maxSize);
        } catch (InterruptedIOException ex) {
            ServerUtilities.LOGGER.info("Backup retention cancelled");
        } catch (IOException ex) {
            ServerUtilities.LOGGER.warn("Skipping legacy backup pruning: {}", ex.getMessage());
        }
    }

    static List<BackupRetention.Archive> readLegacyBackups(File folder, boolean deleteCustom) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) throw new IOException("Cannot list backup folder: " + folder);
        List<BackupRetention.Archive> backupFiles = new ArrayList<>();
        for (File file : files) {
            BackupRetention.checkInterrupted();
            if (!file.getName().endsWith(".zip") || !Files.isRegularFile(file.toPath(), LinkOption.NOFOLLOW_LINKS))
                continue;
            try {
                BackupRetention.Archive archive = BackupRetention.read(file, file.length());
                if (archive.created > System.currentTimeMillis()) {
                    ServerUtilities.LOGGER.warn("Preserving future-dated backup {}", file);
                } else if (deleteCustom || !archive.custom) backupFiles.add(archive);
            } catch (IOException | RuntimeException ex) {
                ServerUtilities.LOGGER.warn("Preserving unrecognized/unreadable backup {}: {}", file, ex.getMessage());
            }
        }
        backupFiles.sort(
                Comparator.comparingLong((BackupRetention.Archive archive) -> archive.file.lastModified())
                        .thenComparing(archive -> archive.file.getName()));
        BackupRetention.checkInterrupted();
        return backupFiles;
    }

    static void clearLegacyBackups(List<BackupRetention.Archive> backupFiles, int keepCount, long maxSize)
            throws InterruptedIOException {
        if (maxSize < 0) {
            ServerUtilities.LOGGER.warn("Skipping legacy backup pruning: negative size limit");
            return;
        }
        long currentSize = backupFiles.stream().mapToLong(archive -> archive.file.length()).sum();
        int remainingCount = backupFiles.size();
        Set<String> worlds = new HashSet<>();
        Set<File> latest = new HashSet<>();
        for (int i = backupFiles.size() - 1; i >= 0; i--) {
            BackupRetention.Archive archive = backupFiles.get(i);
            if (worlds.add(archive.world)) latest.add(archive.file);
        }
        for (BackupRetention.Archive archive : backupFiles) {
            BackupRetention.checkInterrupted();
            if (maxSize > 0 ? currentSize <= maxSize : remainingCount <= Math.max(1, keepCount)) break;
            File file = archive.file;
            if (latest.contains(file)) continue;
            long size = file.length();
            try {
                Files.delete(file.toPath());
                currentSize -= size;
                remainingCount--;
                ServerUtilities.LOGGER.info("Deleted old backup: {}", file);
            } catch (IOException ex) {
                ServerUtilities.LOGGER.warn("Could not delete old backup {}", file, ex);
            }
        }
        if (maxSize > 0 && currentSize > maxSize) {
            ServerUtilities.LOGGER.warn(
                    "Legacy backup size limit could not be met: {} bytes remain; latest backups per world or failed deletions",
                    currentSize);
        } else if (maxSize == 0 && remainingCount > Math.max(1, keepCount)) {
            ServerUtilities.LOGGER
                    .warn("Legacy backup count limit could not be met: {} backups remain", remainingCount);
        }
    }

    private static void pruneRetention(String[] policy, boolean deleteCustom, long maxSize) {
        try {
            BackupRetention.Plan plan = retentionPlan(policy, deleteCustom, maxSize);
            if (!Arrays.equals(policy, backups.retention_policy) || deleteCustom != backups.delete_custom_name_backups
                    || maxSize != backups.max_folder_size * SizeUnit.GB.getSize()) {
                ServerUtilities.LOGGER.info("Retention settings changed; skipping stale pruning plan");
                return;
            }
            clearRetentionBackups(plan, maxSize);
            for (Map.Entry<File, String> decision : plan.keep.entrySet()) {
                if (decision.getValue().startsWith("Unrecognized/unreadable")) {
                    ServerUtilities.LOGGER.warn("Preserving backup {}: {}", decision.getKey(), decision.getValue());
                }
            }
        } catch (InterruptedIOException ex) {
            ServerUtilities.LOGGER.info("Backup retention cancelled");
        } catch (IOException | IllegalArgumentException | ArithmeticException ex) {
            ServerUtilities.LOGGER.warn("Skipping backup pruning: {}", ex.getMessage());
        }
    }

    static void clearRetentionBackups(BackupRetention.Plan plan, long maxSize) throws InterruptedIOException {
        long remaining = 0;
        for (File file : plan.keep.keySet()) {
            if (!plan.sizeExempt.contains(file)) remaining += file.length();
        }
        for (File file : plan.delete.keySet()) remaining += file.length();
        Map<File, String> candidates = new LinkedHashMap<>(plan.delete);
        for (File file : plan.sizeCandidates) candidates.putIfAbsent(file, "Folder size limit");
        for (Map.Entry<File, String> decision : candidates.entrySet()) {
            BackupRetention.checkInterrupted();
            File file = decision.getKey();
            if (!plan.delete.containsKey(file) && (maxSize <= 0 || remaining <= maxSize)) continue;
            long size = file.length();
            try {
                Files.delete(file.toPath());
                remaining -= size;
                ServerUtilities.LOGGER.info("Deleted old backup: {} ({})", file, decision.getValue());
            } catch (IOException ex) {
                ServerUtilities.LOGGER.warn("Could not delete old backup {}", file, ex);
            }
        }
        if (maxSize > 0 && remaining > maxSize) {
            ServerUtilities.LOGGER.warn(
                    "Backup rotation size allowance could not be met: {} counted bytes remain; protected backups or failed deletions",
                    remaining);
        }
    }

    // ponytail: one retention worker serializes scans and deletion; no preview queue to grow without bound.
    static synchronized boolean startRetentionTask(Runnable task) {
        if (backupPreparing || isBackupRunning() || isWorldSavingSuspended()) return false;
        retentionThread = new Thread(() -> {
            try {
                task.run();
            } finally {
                synchronized (BackupTask.class) {
                    if (prunePending && !stopping) {
                        retentionThread = null;
                        clearOldBackups();
                    }
                }
            }
        }, "ServerUtilities backup retention");
        retentionThread.setDaemon(true);
        retentionThread.start();
        return true;
    }

    private static boolean hasRetentionPolicy() {
        return backups.retention_policy != null && backups.retention_policy.length > 0;
    }

    private static BackupRetention.Plan retentionPlan(String[] policy, boolean deleteCustom, long maxSize)
            throws IOException {
        return BackupRetention.plan(BACKUP_FOLDER, policy, System.currentTimeMillis(), deleteCustom, maxSize);
    }

    public static BackupRetention.Plan previewRetention() throws IOException {
        if (!hasRetentionPolicy())
            throw new IllegalArgumentException("retention_policy is empty; legacy retention is active");
        return retentionPlan(
                backups.retention_policy.clone(),
                backups.delete_custom_name_backups,
                backups.max_folder_size * SizeUnit.GB.getSize());
    }

    public static CompletableFuture<BackupRetention.Plan> previewRetentionAsync() {
        if (!hasRetentionPolicy())
            throw new IllegalArgumentException("retention_policy is empty; legacy retention is active");
        String[] policy = backups.retention_policy.clone();
        boolean deleteCustom = backups.delete_custom_name_backups;
        long maxSize = backups.max_folder_size * SizeUnit.GB.getSize();
        return CompletableFuture.supplyAsync(() -> {
            try {
                return retentionPlan(policy, deleteCustom, maxSize);
            } catch (IOException ex) {
                throw new CompletionException(ex);
            }
        },
                task -> {
                    if (!startRetentionTask(task))
                        throw new RejectedExecutionException("Backup or retention scan is already running");
                });
    }

    public static boolean isBackupRunning() {
        Thread backupWorker = thread;
        Thread retentionWorker = retentionThread;
        return stopping || (backupWorker != null && backupWorker.isAlive())
                || (retentionWorker != null && retentionWorker.isAlive());
    }

    public static void stopBackupThread() {
        Thread[] workers;
        synchronized (BackupTask.class) {
            stopping = true;
            workers = new Thread[] { thread, retentionThread };
        }
        boolean interrupted = false;
        for (Thread worker : workers) {
            if (worker == null) continue;
            worker.interrupt();
            while (worker.isAlive()) {
                try {
                    worker.join();
                } catch (InterruptedException ex) {
                    interrupted = true;
                    worker.interrupt();
                }
            }
        }
        restoreWorldSaving();
        synchronized (BackupTask.class) {
            thread = null;
            retentionThread = null;
            backupSucceeded = false;
            prunePending = false;
            stopping = false;
        }
        if (interrupted) Thread.currentThread().interrupt();
    }

    private boolean hasOnlinePlayers(MinecraftServer server) {
        return !server.getConfigurationManager().playerEntityList.isEmpty();
    }

    private void postBackup(Universe universe) {
        if (worldSaveStates.isEmpty()) return;
        if (thread != null && thread.isAlive()) {
            setNextTime(System.currentTimeMillis() + Ticks.SECOND.millis());
            universe.scheduleTask(this);
            return;
        }

        boolean successful = thread == null ? backupSucceeded : thread.successful;
        thread = null;
        backupSucceeded = false;
        restoreWorldSaving();
        if (successful || prunePending) clearOldBackups();
        FileUtils.delete(BACKUP_TEMP_FOLDER);
    }
}
