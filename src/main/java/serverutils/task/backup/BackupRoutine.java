package serverutils.task.backup;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.ThreadedFileIOBase;
import net.minecraftforge.common.DimensionManager;
import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.lib.OtherMods;
import serverutils.lib.data.Universe;
import serverutils.lib.util.CancellationToken;
import serverutils.lib.util.FileUtils;
import serverutils.lib.util.StringUtils;
import serverutils.lib.util.compression.ICompress;


import static serverutils.ServerUtilities.LOGGER;
import static serverutils.ServerUtilitiesConfig.backups;
import static serverutils.ServerUtilitiesNotifications.BACKUP;

// clear separation of concern - routine is the routine (all the logic) for actually saving the world
// do not conflate logic
public class BackupRoutine {
    private static final String DATE_PATTERN = "yyyy-MM-dd-HH-mm-ss";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN, Locale.ROOT);

    private static volatile Instant lastSuccessfulBackup = Instant.MIN;

    private final Map<String, Duration> timings = new LinkedHashMap<>();
    private final CancellationToken cancellation = new CancellationToken();
    private long timing;

    private final Universe universe;
    private final ICompress compressor;
    private final Options options;

    private String backupName;

    private ThreadBackup.Snapshot snapshot;
    private long startedAt;
    private File temporary;

    public BackupRoutine(Universe universe, ICompress compressor, Options options) {
        this.universe = universe;
        this.compressor = compressor;
        this.options = options;
    }

    public static Instant getLastSuccessTime() {
        return lastSuccessfulBackup;
    }

    public void verifyBackupName() {
        if (backupName == null) {
            backupName = determineBackupName(options);
        }
    }

    public CompletableFuture<Void> save(Executor executor) {
        verifyBackupName();
        startedAt = System.nanoTime();
        timing = System.nanoTime();
        preroutine();

        if (!options.useSeparateThread) {
            executor = Runnable::run;
        }

        return CompletableFuture.runAsync(this::doSave, executor);
    }

    public void cancel() {
        cancellation.cancel();
    }

    private void preroutine() {
        boolean prepared = false;
        try {
            // Must run before saveAllChunks so level.dat is written with the current host inventory, otherwise
            // the single-player host's inventory in the backup is stale and items can dupe/vanish on restore.
            universe.server.getConfigurationManager().saveAllPlayerData(); recordTiming("player save");
            universe.disableWorldSaving(); recordTiming("world save");
            flushChunkSaves(universe.server.worldServers); recordTiming("chunk flush");

            if (!backups.silent_backup) {
                BACKUP.sendAll(StringUtils.color("cmd.backup_start", EnumChatFormatting.LIGHT_PURPLE));
            }

//
//            Set<ChunkDimPos> backupChunks = new HashSet<>();
//            boolean onlyClaimed = this.forceOnlyClaimed || backups.only_backup_claimed_chunks;
//            if (onlyClaimed) {
//                if (!ClaimedChunks.isActive()) throw new IllegalStateException("Chunk claiming is not active");
//                ClaimedChunks.instance.processQueue();
//                backupChunks.addAll(ClaimedChunks.instance.getAllClaimedPositions());
//                // noinspection ResultOfMethodCallIgnored
//                BACKUP_TEMP_FOLDER.mkdirs();
//            }

            universe.saveForBackup(); recordTiming("SU data save");
            drainQueuedWrites(); recordTiming("queued data flush");
            recordTiming("setup");

            if (options.snapshotMetaFiles) {
                snapshot = ThreadBackup.snapshotFiles(options.world);
                recordTiming("file snapshot");
            }

            prepared = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (!prepared) cleanup();
        }
    }

    private void doSave() {
        cancellation.enable();
        boolean isIncremental = options.incremental;

        try {
            BackupCandidates candidates = BackupCandidates.scanWorld(options.world, !isIncremental);
            addAdditionalFiles(candidates, options.world);
            candidates.removeMatching(options.excludedBackupFiles);

            if (isIncremental) {
                throw new UnsupportedOperationException("Incremental backups are not supported yet");
            } else {
                doArchiveSave(universe, candidates);
            }
            recordTiming("save");
            lastSuccessfulBackup = Instant.now();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            cleanup();
            logTiming();
        }
    }

    private void doSnapshotSave(Universe universe) {
        // implement in future PR
    }

    private void doArchiveSave(Universe universe, BackupCandidates candidates) throws IOException {
        File destination = prepareDestination(backupName, options.incremental ? ".json" : ".zip");
        temporary = FileUtils.createSaveTemporary(destination.toPath().toAbsolutePath()).toFile();

        compressor.createOutputStream(temporary);
        BackupRetention.writeMetadata(compressor, universe.getUUID().toString(), options.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), options.customName != null);

        for (WorldFile file : candidates.getFiles()) {
            cancellation.check();
            // Restore expects run-folder-relative entries i.e. saves/<world>/level.dat
            // not world-relative ones, so the world name prefix must be kept.
            compressor.addFileToArchive(file.backing(), FileUtils.getRelativePath(file.backing()));
        }

        try {
            compressor.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Path temporaryPath = temporary.toPath();
        destination = prepareDestination(backupName, options.incremental ? ".json" : ".zip");
        Path destinationPath = destination.toPath();

        cancellation.check();
        if (options.overwrite) {
            if (destination.exists() && Files.getFileAttributeView(destinationPath, PosixFileAttributeView.class) != null) {
                Files.setPosixFilePermissions(temporaryPath, Files.getPosixFilePermissions(destinationPath));
            }
            // TODO: backup
            Files.move(temporaryPath, destinationPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } else {
            // ATOMIC_MOVE may replace an existing file even without REPLACE_EXISTING.
            Files.move(temporaryPath, destinationPath);
        }

        String backupSize = FileUtils.getSizeString(destination);
        LOGGER.info("Backup done in {} seconds ({})!", getDoneTime(startedAt), backupSize);
        LOGGER.info("Created {} from {}", destination.getAbsolutePath(), options.world.getAbsolutePath());

        if (backups.display_file_size) {
            String sizeT = FileUtils.getSizeString(options.backupsDirectory);
            BACKUP.sendAll(
                StringUtils.color(
                    "cmd.backup_end_2",
                    EnumChatFormatting.LIGHT_PURPLE,
                    getDoneTime(startedAt),
                    (backupSize.equals(sizeT) ? backupSize : (backupSize + " | " + sizeT))));
        } else {
            BACKUP.sendAll(
                StringUtils.color("cmd.backup_end_1", EnumChatFormatting.LIGHT_PURPLE, getDoneTime(startedAt)));
        }
    }

    private void cleanup() {
        try {
            cancellation.disable();
        } catch (Exception e) {
            LOGGER.error("Failed to disable cancellation", e);
        }

        try {
            compressor.close();
        } catch (Exception e) {
            LOGGER.error("Failed to close compressor", e);
        }

        try {
            if (temporary != null) Files.deleteIfExists(temporary.toPath());
        } catch (IOException e) {
            LOGGER.warn("Could not delete partial archive {}", temporary, e);
        }

        try {
            FileUtils.delete(BackupTask.BACKUP_TEMP_FOLDER);
        } catch (RuntimeException e) {
            LOGGER.warn("Could not delete temporary backup folder", e);
        }

        try {
            universe.restoreWorldSaving();
        } catch (RuntimeException e) {
            LOGGER.warn("Could not restore world saving", e);
        }
    }

    private void recordTiming(String phase) {
        long now = System.nanoTime();
        long deltaTime = now - timing;
        timings.put(phase, Duration.ofNanos(deltaTime));
        timing = now;
    }

    private void logTiming() {
        StringBuilder timings = new StringBuilder();
        for (Map.Entry<String, Duration> entry : this.timings.entrySet()) {
            if (timings.length() > 0) timings.append("; ");
            timings.append(entry.getKey()).append('=').append(entry.getValue().toMillis()).append(" ms");
        }
        LOGGER.info(
            "Backup server-thread timing: started={}; total={} ms; {}",
            startedAt,
            (System.nanoTime() - startedAt) / 1_000_000L,
            timings);
    }

    /** Adds the ranks files and everything matched by the configured additional_backup_files. */
    private void addAdditionalFiles(BackupCandidates candidates, File world) throws IOException {
        for (String name : new String[] { "ranks.txt", "players.txt" }) {
            File file = new File(ServerUtilities.SERVER_FOLDER, name);
            if (file.isFile()) candidates.addFile(file);
        }
        for (String pattern : options.additionalBackupFiles) {
            candidates.addFiles(FileUtils.findMatching(
                pattern.replace("$WORLDNAME", world.getName()),
                options.backupsDirectory.toPath(),
                BackupTask.BACKUP_TEMP_FOLDER.toPath()));
        }
    }

    private File prepareDestination(String filename, String extension) {
        // when the filename ends with an extension
        // it's already been verified (custom names) and therefore we can just return a new file at the destination
        if (filename.endsWith(extension)) return new File(options.backupsDirectory, filename);

        for (int i = 0; true; i++) {
            File destination = new File(options.backupsDirectory, filename + (i == 0 ? "" : "-" + i) + extension);
            if (!destination.exists()) return destination;
        }
    }

    private static void flushChunkSaves(WorldServer[] worlds) throws InterruptedException {
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

    /** Reports retained Hodgepodge failures without requiring that mod, or its newer flush API. */
    private static void drainQueuedWrites() throws Exception {
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

    private static void flushWorldDataSaver(Class<?> saver) throws Exception {
        Method flush;
        try {
            flush = saver.getMethod("flush");
        } catch (NoSuchMethodException e) {
            LOGGER.warn(
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

    private static String determineBackupName(Options options) {
        String backupName = options.customName;
        if (backupName == null) backupName = "";
        boolean overwrite = options.overwrite;
        LocalDateTime createdAt = options.createdAt;

        if (backupName.isEmpty()) {
            if (overwrite) throw new IllegalArgumentException("Overwrite requires a custom backup name");
            backupName = createdAt.format(FORMATTER);
        } else {
            validateBackupName(backupName);
            backupName += (options.incremental ? ".json" : ".zip");
            File destination = new File(options.backupsDirectory, backupName);
            if (!overwrite && destination.exists()) {
                throw new IllegalArgumentException("Backup already exists: " + backupName + ". Use =overwrite to replace it explicitly");
            }
            if (!destination.getParentFile().equals(options.backupsDirectory)) {
                throw new IllegalArgumentException("Backup destination must be in the backups directory");
            }
            Path destinationPath = destination.toPath();
            if (Files.exists(destinationPath, LinkOption.NOFOLLOW_LINKS) && !Files.isRegularFile(destinationPath, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException("Backup destination must be a regular file");
            }
        }

        return backupName;
    }

    private static void validateBackupName(String name) {
        if (name == null || name.chars().anyMatch(c -> c < 32 || c == 127 || "\\/:*?\"<>|".indexOf(c) >= 0)) {
            throw new IllegalArgumentException("Backup name must be a filename without path separators or reserved characters");
        }
    }

    private static String getDoneTime(long l) {
        return StringUtils.getTimeString(System.nanoTime() - l);
    }

    public static final class Options {
        public File backupsDirectory;
        public File world;
        public boolean onlyClaimed;
        public boolean incremental;
        public boolean snapshotMetaFiles;
        public boolean useSeparateThread;
        public String customName;
        public boolean overwrite;
        public LocalDateTime createdAt;
        public List<String> additionalBackupFiles;
        public List<String> excludedBackupFiles;

        public Options(
            File backupsDirectory,
            File world,
            boolean onlyClaimed,
            boolean incremental,
            boolean snapshotMetaFiles,
            boolean useSeparateThread,
            List<String> additionalBackupFiles,
            List<String> excludedBackupFiles,
            String customName,
            boolean overwrite,
            LocalDateTime createdAt
        ) {
            this.backupsDirectory = backupsDirectory;
            this.world = world;
            this.onlyClaimed = onlyClaimed;
            this.incremental = incremental;
            this.snapshotMetaFiles = snapshotMetaFiles;
            this.useSeparateThread = useSeparateThread;
            this.additionalBackupFiles = additionalBackupFiles;
            this.excludedBackupFiles = excludedBackupFiles;
            this.customName = customName;
            this.overwrite = overwrite;
            this.createdAt = createdAt;

        }

        public static Options fromConfig() {
            return new Options(
                BackupTask.BACKUP_FOLDER,
                DimensionManager.getCurrentSaveRootDirectory(),
                ServerUtilitiesConfig.backups.only_backup_claimed_chunks,
                false,
                !backups.prefer_speed_over_backup_consistency,
                backups.use_separate_thread,
                Arrays.asList(backups.additional_backup_files),
                Arrays.asList(backups.excluded_backup_files),
                null,
                false,
                LocalDateTime.now()
            );
        }
    }
}
