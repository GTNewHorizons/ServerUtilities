package serverutils.task.backup;

import static serverutils.ServerUtilities.LOGGER;
import static serverutils.ServerUtilitiesConfig.backups;
import static serverutils.ServerUtilitiesNotifications.BACKUP;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.ThreadedFileIOBase;
import net.minecraftforge.common.DimensionManager;

import org.jetbrains.annotations.NotNull;

import lombok.Builder;
import lombok.Getter;
import serverutils.ServerUtilities;
import serverutils.data.ClaimedChunks;
import serverutils.lib.OtherMods;
import serverutils.lib.data.Universe;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.util.CancellationToken;
import serverutils.lib.util.FileUtils;
import serverutils.lib.util.ServerUtils;
import serverutils.lib.util.StringUtils;
import serverutils.lib.util.WorldRegion;
import serverutils.lib.util.compression.ICompress;

// clear separation of concern - routine is the routine (all the logic) for actually saving the world
// do not conflate logic
public class BackupRoutine {

    private static final String DATE_PATTERN = "yyyy-MM-dd-HH-mm-ss";
    private static final Set<String> BQ_DATABASES = new HashSet<>(
            Arrays.asList("QuestDatabase", "QuestProgress", "QuestingParties", "NameCache", "LifeDatabase"));

    private static volatile Instant lastSuccessfulBackup = Instant.MIN;

    private final Map<String, Duration> timings = new LinkedHashMap<>();
    private final CancellationToken cancellation = new CancellationToken();
    private final Map<String, Map<String, List<ChunkDimPos>>> dimToRegionToChunkPos = new HashMap<>();

    /** Archive entry name -> copy taken while world saving was suspended. */
    private final Map<String, File> snapshot = new HashMap<>();

    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN, Locale.ROOT);

    private long timing;

    private final Universe universe;
    private final Options options;

    private String backupName;

    @Getter
    private boolean prepared;
    private Instant startedAtInstant;
    private long startedAt;
    private File temporary;
    private ICompress compressor;

    private Supplier<ICompress> compressorSupplier = ICompress::createCompressor;
    private BackupCandidates candidates;

    public static final File BACKUP_TEMP_FOLDER = new File("serverutilities/temp/");

    static {
        ServerUtilities.LOGGER.info("Backups folder - {}", getDefaultBackupFolder().getAbsolutePath());
    }

    public static File getDefaultBackupFolder() {
        File folder = backups.backup_folder_path.isEmpty() ? new File("/backups/")
                : new File(backups.backup_folder_path);
        if (!folder.exists()) folder.mkdirs();
        return folder;
    }

    public BackupRoutine(Universe universe, Options options, Supplier<ICompress> compressorSupplier) {
        this.universe = universe;
        this.compressorSupplier = compressorSupplier;
        this.options = options;
    }

    public BackupRoutine(Universe universe, Options options) {
        this.universe = universe;
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

    public BackupResult.Save save() {
        try {
            return save(Runnable::run).get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException rte) throw rte;
            throw new RuntimeException(e.getCause());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public CompletableFuture<BackupResult.Save> save(Executor executor) {
        BackupResult.Save result = new BackupResult.Save(options.world, options.incremental);

        verifyBackupName();

        startedAtInstant = Instant.now();
        startedAt = System.nanoTime();
        timing = System.nanoTime();
        compressor = compressorSupplier.get();

        prepared = preroutine(result);
        if (!prepared) {
            return CompletableFuture.completedFuture(result.finished());
        }

        if (!options.useSeparateThread) {
            executor = Runnable::run;
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                doSave(result);
            } catch (Throwable t) {
                result.error = t;
                if (isCancellation(t)) {
                    LOGGER.info("Backup cancelled, deleting partial archive");
                } else {
                    LOGGER.error("An error occurred while saving backup", t);
                    ServerUtils.notifyChat(
                            universe.server,
                            null,
                            StringUtils.color("cmd.backup_fail", EnumChatFormatting.RED, t.getMessage()));
                }
            } finally {
                cleanup();
                logTiming();
            }
            return result.finished();
        }, executor);
    }

    public void cancel() {
        cancellation.cancel();
    }

    private boolean isCancellation(Throwable t) {
        return cancellation.isCancelled() || t instanceof InterruptedIOException
                || t instanceof ClosedByInterruptException;
    }

    private boolean preroutine(BackupResult.Save result) {
        boolean prepared = false;
        try {
            // Must run before saveAllChunks so level.dat is written with the current host inventory, otherwise
            // the single-player host's inventory in the backup is stale and items can dupe/vanish on restore.
            universe.server.getConfigurationManager().saveAllPlayerData();
            recordTiming("player save");
            universe.disableWorldSaving();
            recordTiming("world save");

            // reordered because flushing chunks takes time and world saving has already been disabled
            if (!options.silent) {
                BACKUP.sendAll(StringUtils.color("cmd.backup_start", EnumChatFormatting.LIGHT_PURPLE));
            }

            flushChunkSaves(universe.server.worldServers);
            recordTiming("chunk flush");

            if (options.onlyClaimed) {
                if (!ClaimedChunks.isActive()) throw new IllegalStateException("Chunk claiming is not active");
                FileUtils.ensureExists(BACKUP_TEMP_FOLDER);
                ClaimedChunks.instance.processQueue();
                ClaimedChunks.instance.getAllClaimedPositions().forEach(pos -> {
                    String dimName = pos.dim == 0 ? BackupCandidates.OVERWORLD : "DIM" + pos.dim;
                    // pre-compute and map chunk positions to their specific regions
                    Map<String, List<ChunkDimPos>> positions = dimToRegionToChunkPos
                            .computeIfAbsent(dimName, dim -> new HashMap<>());
                    String region = "r." + (pos.posX >> 5) + "." + (pos.posZ >> 5);
                    positions.computeIfAbsent(region, r -> new ArrayList<>()).add(pos);
                });
                if (dimToRegionToChunkPos.isEmpty()) {
                    LOGGER.warn("Claim-only backup has no claimed chunks; known dimension regions will be omitted");
                }
            }

            universe.saveForBackup();
            recordTiming("SU data save");
            drainQueuedWrites();
            recordTiming("queued data flush");
            recordTiming("setup");

            if (options.snapshotMetaFiles && options.useSeparateThread) {
                candidates = getCandidates();
                snapshotFiles(candidates);
                recordTiming("file snapshot");
            }

            prepared = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result.error = e;
        } catch (Exception e) {
            ServerUtils.notifyChat(
                    universe.server,
                    null,
                    new ChatComponentText(
                            EnumChatFormatting.RED + "An error occurred while preparing backup. " + e.getMessage()));
            ServerUtilities.LOGGER.info("An error occurred while preparing backup, Aborting!", e);
            result.error = e;
        } finally {
            if (!prepared) cleanup();
        }
        return prepared;
    }

    private void doSave(BackupResult.Save result) throws IOException {
        cancellation.enable();
        boolean isIncremental = options.incremental;

        if (candidates == null) candidates = getCandidates();
        LOGGER.info("Performing {} backup of {} files...", isIncremental ? "incremental" : "full", candidates.count());
        if (isIncremental) {
            throw new UnsupportedOperationException("Incremental backups are not supported yet");
        } else {
            doArchiveSave(universe, candidates, result);
        }
        recordTiming("save");
        lastSuccessfulBackup = startedAtInstant;
    }

    private void doSnapshotSave(Universe universe) {
        // implement in future PR
    }

    private void doArchiveSave(Universe universe, BackupCandidates candidates, BackupResult.Save result)
            throws IOException {
        File destination = prepareDestination(backupName, ".zip");
        temporary = FileUtils.createSaveTemporary(destination.toPath().toAbsolutePath()).toFile();

        compressor.createOutputStream(temporary);
        ArchiveStore.writeMetadata(
                compressor,
                universe.getUUID().toString(),
                options.createdAt.toEpochMilli(),
                options.customName != null);

        List<WorldFile> files = candidates.getFiles();
        int fileSize = files.size();
        long logTimer = System.currentTimeMillis();

        int regionCount = 0;
        int chunkCount = 0;

        for (int i = 0; i < fileSize; i++) {
            WorldFile file = files.get(i);
            cancellation.check();
            // Restore expects run-folder-relative entries i.e. saves/<world>/level.dat
            // not world-relative ones, so the world name prefix must be kept.
            String entry = FileUtils.getRelativePath(file.backing());

            if (i == fileSize - 1 || System.currentTimeMillis() >= logTimer) {
                logTimer = System.currentTimeMillis() + 5000;
                LOGGER.info(
                        "[{} | {}%]: {}",
                        i,
                        StringUtils.formatDouble00((i / (double) fileSize) * 100D),
                        file.backing().getAbsolutePath());
            }

            List<ChunkDimPos> claims = options.onlyClaimed && options.writeOnlyClaimedChunks ? getClaims(file) : null;
            if (claims != null && canTrimRegion(file)) {
                File tempRegionFile = Files.createTempFile(BACKUP_TEMP_FOLDER.toPath(), "claimed-", ".mca").toFile();
                try {
                    int chunks;
                    try (WorldRegion region = new WorldRegion(file);
                            WorldRegion tempRegion = new WorldRegion(tempRegionFile, region.dimension)) {
                        chunks = region.copyTo(tempRegion, claims);
                    }

                    if (chunks > 0) {
                        compressor.addFileToArchive(tempRegionFile, entry);
                        result.newFiles++;
                        regionCount++;
                        chunkCount += chunks;
                    }
                } finally {
                    Files.deleteIfExists(tempRegionFile.toPath());
                }
                continue;
            }

            compressor.addFileToArchive(snapshot.getOrDefault(entry, file.backing()), entry);
            result.newFiles++;
        }

        if (options.onlyClaimed) {
            LOGGER.info("Backed up {} regions containing {} claimed chunks", regionCount, chunkCount);
        }

        try {
            compressor.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Path temporaryPath = temporary.toPath();
        Path destinationPath = destination.toPath();

        cancellation.check();
        if (options.overwrite) {
            if (destination.exists()
                    && Files.getFileAttributeView(destinationPath, PosixFileAttributeView.class) != null) {
                if (!Files.isRegularFile(destinationPath))
                    throw new IllegalStateException("Destination is not a regular file");
                Files.setPosixFilePermissions(temporaryPath, Files.getPosixFilePermissions(destinationPath));
            }

            Files.move(
                    temporaryPath,
                    destinationPath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } else {
            // ATOMIC_MOVE may replace an existing file even without REPLACE_EXISTING.
            Files.move(temporaryPath, destinationPath);
        }

        result.bytes = FileUtils.getSize(destination);
        result.destination = destination;
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
            FileUtils.delete(BACKUP_TEMP_FOLDER);
        } catch (RuntimeException e) {
            LOGGER.warn("Could not delete temporary backup folder", e);
        }

        universe.scheduleAction(() -> {
            try {
                universe.restoreWorldSaving();
            } catch (RuntimeException e) {
                LOGGER.warn("Could not restore world saving", e);
            }
        });

        cancellation.clear();
        snapshot.clear();
        dimToRegionToChunkPos.clear();
        timings.clear();
        candidates = null;
        backupName = null;
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
            candidates.addFiles(
                    FileUtils.findMatching(
                            pattern.replace("$WORLDNAME", world.getName()),
                            options.backupsDirectory.toPath(),
                            BACKUP_TEMP_FOLDER.toPath()));
        }
    }

    private File prepareDestination(String filename, String extension) {
        // when the filename ends with an extension
        // it's already been verified (custom names) and therefore we can just return a new file at the destination
        if (filename.endsWith(extension)) return new File(options.getWorldBackupDirectory(), filename);

        return FileUtils.getFileWithoutCollision(
                options.getWorldBackupDirectory(),
                filename,
                extension,
                FileUtils.INCREMENTAL_APPEND);
    }

    private BackupCandidates getCandidates() throws IOException {
        BackupCandidates candidates = BackupCandidates
                .scanWorld(options.world, options.backupsDirectory, !options.incremental);

        // keep only misc files + region files with claims in them
        if (options.onlyClaimed) {
            Set<String> unknownDimensions = new HashSet<>();
            candidates.keep(f -> {
                // don't remove misc files
                if (f.kind() == WorldFile.Kind.MISC) return true;
                // Claims are keyed as overworld/DIM<id>, so a dimension folder with any other name can't be matched
                // to its claims. Keep its regions whole rather than dropping claimed chunks.
                if (!isNumberedDimension(f.dimension)) {
                    unknownDimensions.add(f.dimension);
                    return true;
                }
                return getClaims(f) != null;
            });

            for (String dimension : unknownDimensions) {
                LOGGER.warn("Cannot identify dimension for region files in {}; copying them unchanged", dimension);
            }
        }

        addAdditionalFiles(candidates, options.world);
        candidates.removeMatching(options.excludedBackupFiles, options.world.getName());
        return candidates;
    }

    /**
     * Copies the files mods may rewrite while the archive is being written, so the backup sees them as they were while
     * world saving was suspended. Region files and stable mod data are read live instead.
     */
    private void snapshotFiles(BackupCandidates candidates) throws IOException {
        Path world = options.world.toPath().toAbsolutePath().normalize();
        File folder = new File(BACKUP_TEMP_FOLDER, "snapshot");
        FileUtils.delete(folder);
        FileUtils.ensureExists(folder);

        Predicate<WorldFile> isSnapshottable = f -> {
            if (f.kind() != WorldFile.Kind.MISC) return false;
            if (isStableModData(f, world)) return false;
            if (f.dimension == null) return false;
            return !FileUtils.isRegionFile(f.backing());
        };

        for (WorldFile file : candidates.getFiles(isSnapshottable)) {
            File source = file.backing();
            // Mirror the world layout, no name is longer than the original, and paths can't collide
            File copy = new File(folder, world.relativize(source.toPath().toAbsolutePath().normalize()).toString());
            FileUtils.ensureExists(copy.getParentFile());
            long size = source.length();
            long modified = source.lastModified();
            Files.copy(source.toPath(), copy.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            if (source.length() != size || source.lastModified() != modified || copy.length() != size) {
                throw new IOException("File changed during backup snapshot: " + source);
            }
            // store a reference to the new file based on its candidate path
            snapshot.put(FileUtils.getRelativePath(file.backing()), copy);
        }
    }

    /**
     * Mod files that are only rewritten on world save/shutdown (or migration), so reading them live at archive time is
     * as consistent as a snapshot. Only the world's own files qualify, never links to somewhere else.
     */
    private static boolean isStableModData(WorldFile file, Path world) {
        Path path = file.backing().toPath().toAbsolutePath().normalize();
        if (!path.startsWith(world)) return false;
        Path relative = world.relativize(path);
        String name = file.name();

        boolean stable;
        if (relative.getNameCount() == 3 && relative.startsWith(Paths.get("AE2", "spawndata"))) {
            // AE2 flushes meteor spawn data on world saves and shutdown, not on ordinary gameplay updates.
            // World saving stays suspended, and a backup cancelled at shutdown never publishes its archive.
            stable = name.endsWith(".dat");
        } else if (relative.getNameCount() == 4 && relative.startsWith(Paths.get("betterquesting", "backup"))) {
            // BQ's versioned migration copies are written during database loading. Live BQ files remain captured.
            String suffix = "_backup_" + relative.getName(2) + ".json";
            stable = name.endsWith(suffix) && BQ_DATABASES.contains(name.substring(0, name.length() - suffix.length()));
        } else {
            return false;
        }
        if (!stable) return false;

        // Candidates follow links, so confirm this really is the world's own file and not a link elsewhere.
        try {
            return path.toRealPath().equals(world.toRealPath().resolve(relative));
        } catch (IOException e) {
            return false;
        }
    }

    /** The claimed chunks inside this region file, or null if it is not a region file or holds no claims. */
    private List<ChunkDimPos> getClaims(WorldFile file) {
        if (file.kind() != WorldFile.Kind.REGION) return null;
        Map<String, List<ChunkDimPos>> regions = dimToRegionToChunkPos.get(file.dimension);
        return regions == null ? null : regions.get(file.name().replace(".mca", ""));
    }

    private static boolean isNumberedDimension(String dimension) {
        return BackupCandidates.OVERWORLD.equals(dimension) || (dimension != null && dimension.matches("DIM-?\\d+"));
    }

    /** RegionFile opens read-write and pads malformed files in its constructor, so those are never opened. */
    private static boolean canTrimRegion(WorldFile file) {
        long size = file.size();
        if (size >= 8192 && size % 4096 == 0) return true;
        LOGGER.warn("Cannot trim malformed region {}; copying it unchanged", file.backing());
        return false;
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

    static void flushWorldDataSaver(Class<?> saver) throws Exception {
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
        if (options.incremental) return backupName;
        if (backupName == null) backupName = "";
        boolean overwrite = options.overwrite;
        ZonedDateTime createdAt = options.createdAt.atZone(ZoneId.systemDefault());

        if (backupName.isEmpty()) {
            if (overwrite) throw new IllegalArgumentException("Overwrite requires a custom backup name");
            // set the backup name without extension so that prepareDestination can find a non-conflicting destination
            backupName = createdAt.format(DATE_FORMATTER);
        } else {
            validateBackupName(backupName);
            backupName += ".zip";
            Path destinationPath = getDestinationPath(options, backupName, overwrite);
            if (Files.exists(destinationPath, LinkOption.NOFOLLOW_LINKS)
                    && !Files.isRegularFile(destinationPath, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException("Backup destination must be a regular file");
            }
        }

        return backupName;
    }

    private static @NotNull Path getDestinationPath(Options options, String backupName, boolean overwrite) {
        File destination = new File(options.getWorldBackupDirectory(), backupName);
        if (!overwrite && destination.exists()) {
            throw new IllegalArgumentException(
                    "Backup already exists: " + backupName + ". Use =overwrite to replace it explicitly");
        }
        if (!destination.getParentFile().equals(options.backupsDirectory)) {
            throw new IllegalArgumentException("Backup destination must be in the backups directory");
        }
        return destination.toPath();
    }

    private static void validateBackupName(String name) {
        if (name == null || name.chars().anyMatch(c -> c < 32 || c == 127 || "\\/:*?\"<>|".indexOf(c) >= 0)) {
            throw new IllegalArgumentException(
                    "Backup name must be a filename without path separators or reserved characters");
        }
    }

    @Builder
    public static final class Options {

        public File backupsDirectory;
        public File world;
        public boolean onlyClaimed;
        public boolean writeOnlyClaimedChunks;
        public boolean incremental;
        public boolean snapshotMetaFiles;
        public boolean useSeparateThread;
        public boolean silent;
        public String customName;
        public boolean overwrite;
        public Instant createdAt;
        public List<String> additionalBackupFiles;
        public List<String> excludedBackupFiles;

        public static Options fromConfig() {
            return Options.builder().backupsDirectory(getDefaultBackupFolder())
                    .world(DimensionManager.getCurrentSaveRootDirectory())
                    .onlyClaimed(backups.only_backup_claimed_chunks)
                    .writeOnlyClaimedChunks(!backups.backup_entire_regions_with_claims)
                    .snapshotMetaFiles(!backups.prefer_speed_over_backup_consistency)
                    .useSeparateThread(backups.use_separate_thread)
                    .additionalBackupFiles(Arrays.asList(backups.additional_backup_files))
                    .excludedBackupFiles(Arrays.asList(backups.excluded_backup_files)).overwrite(false)
                    .silent(backups.silent_backup).createdAt(Instant.now()).build();
        }

        public File getWorldBackupDirectory() {
            if (incremental) return new File(backupsDirectory, world.getName());
            return backupsDirectory;
        }
    }
}
