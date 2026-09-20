package serverutils.task.backup;

import static serverutils.ServerUtilitiesConfig.backups;
import static serverutils.ServerUtilitiesNotifications.BACKUP;
import static serverutils.task.backup.BackupTask.BACKUP_TEMP_FOLDER;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.storage.RegionFile;
import net.minecraftforge.common.DimensionManager;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import serverutils.ServerUtilities;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.FileUtils;
import serverutils.lib.util.ServerUtils;
import serverutils.lib.util.StringUtils;
import serverutils.lib.util.compression.ICompress;

public class ThreadBackup extends Thread {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    private static long logMillis;
    private final File src0;
    private final String customName;
    private final Set<ChunkDimPos> chunksToBackup;
    private final ICompress compressor;
    private final Snapshot snapshot;
    private final boolean onlyClaimed;
    private final Map<Integer, File> dimensionFolders;

    public ThreadBackup(ICompress compress, File sourceFile, String backupName, Set<ChunkDimPos> backupChunks) {
        this(compress, sourceFile, backupName, backupChunks, null);
    }

    ThreadBackup(ICompress compress, File sourceFile, String backupName, Set<ChunkDimPos> backupChunks,
            Snapshot snapshot) {
        this(
                compress,
                sourceFile,
                backupName,
                backupChunks,
                snapshot,
                backups.only_backup_claimed_chunks && !backupChunks.isEmpty());
    }

    ThreadBackup(ICompress compress, File sourceFile, String backupName, Set<ChunkDimPos> backupChunks,
            Snapshot snapshot, boolean onlyClaimed) {
        src0 = sourceFile;
        customName = backupName;
        chunksToBackup = new HashSet<>(backupChunks);
        compressor = compress;
        this.snapshot = snapshot;
        // Capture provider paths on the calling server thread before starting the backup worker.
        dimensionFolders = onlyClaimed ? resolveDimensionFolders(sourceFile) : Collections.emptyMap();
        this.onlyClaimed = onlyClaimed;
        setPriority(7);
    }

    public void run() {
        try {
            doBackup(compressor, src0, customName, chunksToBackup, snapshot, onlyClaimed, dimensionFolders);
        } finally {
            if (snapshot != null) deleteSnapshot();
        }
    }

    private static void addBaseFolderFiles(Map<String, File> files, File saveFile) throws IOException {
        String saveName = saveFile.getName();

        for (String pattern : backups.additional_backup_files) {
            pattern = FileUtils.normalizeBackupPattern(pattern.replace("$WORLDNAME", saveName));

            int firstWildcardIndex = pattern.indexOf('*');
            if (firstWildcardIndex == -1) {
                for (File file : listOutsideBackupStorage(new File(pattern)).keySet()) {
                    files.putIfAbsent(FileUtils.getRelativePath(file), file);
                }
                continue;
            }

            Path rootFolder = Paths.get(pattern.substring(0, firstWildcardIndex));

            // If wildcard was not at the start of a directory, get the parent
            if (firstWildcardIndex != 0 && (pattern.charAt(firstWildcardIndex - 1) != '/')) {
                rootFolder = rootFolder.getParent();
            }
            if (rootFolder == null || rootFolder.toString().isEmpty()) rootFolder = Paths.get(".");

            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            Map<File, BasicFileAttributes> fileCandidates = listOutsideBackupStorage(
                    rootFolder.toFile(),
                    matcher,
                    backupGlobTraversal(pattern));
            for (File file : fileCandidates.keySet()) {
                if (matcher.matches(file.toPath().normalize())) {
                    files.putIfAbsent(FileUtils.getRelativePath(file), file);
                }
            }
        }
    }

    /** Matches directories that can contain selected files. The full glob separately selects the files themselves. */
    static PathMatcher backupGlobTraversal(String pattern) {
        List<PathMatcher> prefixes = new ArrayList<>();
        int group = -1;
        boolean characterClass = false;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '[') characterClass = true;
            if (c == ']') characterClass = false;
            if (characterClass) continue;
            if (c == '{') group = i;
            if (c == '}') group = -1;
            // A recursive wildcard can consume separators. Alternatives containing separators need the same
            // conservative traversal from their opening brace; the full matcher still selects the actual files.
            if ((c == '*' && i + 1 < pattern.length() && pattern.charAt(i + 1) == '*') || (c == '/' && group >= 0)) {
                prefixes.add(
                        FileSystems.getDefault()
                                .getPathMatcher("glob:" + pattern.substring(0, group >= 0 ? group : i) + "**"));
                break;
            }
            if (c == '/' && i > 0) {
                prefixes.add(FileSystems.getDefault().getPathMatcher("glob:" + pattern.substring(0, i)));
            }
        }
        return path -> {
            Path normalized = path.normalize();
            return prefixes.stream().anyMatch(prefix -> prefix.matches(normalized));
        };
    }

    public static void doBackup(ICompress compressor, File src, String customName, Set<ChunkDimPos> chunks) {
        doBackup(compressor, src, customName, chunks, null);
    }

    static void doBackup(ICompress compressor, File src, String customName, Set<ChunkDimPos> chunks,
            Snapshot snapshot) {
        doBackup(
                compressor,
                src,
                customName,
                chunks,
                snapshot,
                backups.only_backup_claimed_chunks && !chunks.isEmpty());
    }

    static void doBackup(ICompress compressor, File src, String customName, Set<ChunkDimPos> chunks, Snapshot snapshot,
            boolean onlyClaimed) {
        doBackup(compressor, src, customName, chunks, snapshot, onlyClaimed, null);
    }

    private static void doBackup(ICompress compressor, File src, String customName, Set<ChunkDimPos> chunks,
            Snapshot snapshot, boolean onlyClaimed, Map<Integer, File> dimensionFolders) {
        String outName = (customName.isEmpty() ? DATE_FORMAT.format(Calendar.getInstance().getTime()) : customName)
                + ".zip";
        File dstFile = null;
        Path temporary = null;
        try {
            validateBackupSource(src);
            if (onlyClaimed && chunks.isEmpty()) {
                ServerUtilities.LOGGER
                        .warn("Claim-only backup has no claimed chunks; known dimension regions will be omitted");
            }
            if (onlyClaimed && dimensionFolders == null) {
                dimensionFolders = resolveDimensionFolders(src);
            }
            Map<String, File> files = snapshot == null ? listWorldFiles(src, null)
                    : new LinkedHashMap<>(snapshot.files);
            addBaseFolderFiles(files, src);
            long start = System.currentTimeMillis();
            logMillis = start + Ticks.SECOND.x(5).millis();

            dstFile = new File(BackupTask.BACKUP_FOLDER, outName);
            Path destination = dstFile.toPath().toAbsolutePath();
            Files.createDirectories(destination.getParent());
            temporary = FileUtils.createSaveTemporary(destination);
            try (compressor) {
                compressor.createOutputStream(temporary.toFile());
                int captured = snapshot == null ? 0 : compressSnapshot(snapshot, files, compressor);
                if (onlyClaimed) {
                    backupRegions(files, src, chunks, compressor, dimensionFolders, captured);
                } else {
                    compressFiles(files, compressor, captured);
                }

            }
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Backup cancelled");
            if (Files.exists(destination)
                    && Files.getFileAttributeView(destination, PosixFileAttributeView.class) != null) {
                Files.setPosixFilePermissions(temporary, Files.getPosixFilePermissions(destination));
            }
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            temporary = null;
            String backupSize = FileUtils.getSizeString(dstFile);
            ServerUtilities.LOGGER.info("Backup done in {} seconds ({})!", getDoneTime(start), backupSize);
            ServerUtilities.LOGGER.info("Created {} from {}", dstFile.getAbsolutePath(), src.getAbsolutePath());

            if (!backups.silent_backup) {
                if (backups.display_file_size) {
                    String sizeT = FileUtils.getSizeString(BackupTask.BACKUP_FOLDER);
                    BACKUP.sendAll(
                            StringUtils.color(
                                    "cmd.backup_end_2",
                                    EnumChatFormatting.LIGHT_PURPLE,
                                    getDoneTime(start),
                                    (backupSize.equals(sizeT) ? backupSize : (backupSize + " | " + sizeT))));
                } else {
                    BACKUP.sendAll(
                            StringUtils.color("cmd.backup_end_1", EnumChatFormatting.LIGHT_PURPLE, getDoneTime(start)));
                }
            }
        } catch (InterruptedIOException e) {
            ServerUtilities.LOGGER.info("Backup cancelled, deleting partial archive");
        } catch (Exception e) {
            ServerUtilities.LOGGER.error("Error while backing up", e);
            ServerUtils.notifyChat(
                    ServerUtils.getServer(),
                    null,
                    StringUtils.color("cmd.backup_fail", EnumChatFormatting.RED, e.getMessage()));

        } finally {
            if (temporary != null) FileUtils.delete(temporary.toFile());
        }
    }

    static final class Snapshot {

        final Map<String, File> files;
        final File spool;
        final List<ZipEntry> entries = new ArrayList<>();

        Snapshot(Map<String, File> files, File spool) {
            this.files = files;
            this.spool = spool;
        }
    }

    static Snapshot snapshotFiles(File src) throws IOException {
        long started = System.nanoTime();
        validateBackupSource(src);
        deleteSnapshot();
        if (!BACKUP_TEMP_FOLDER.mkdirs() && !BACKUP_TEMP_FOLDER.isDirectory()) {
            throw new IOException("Could not create backup staging directory");
        }

        Map<File, BasicFileAttributes> listedAttributes = new HashMap<>();
        Map<String, File> files = listWorldFiles(src, listedAttributes);
        long listed = System.nanoTime();
        Path world = src.toPath().toAbsolutePath().normalize();
        Path realWorld = world.toRealPath();
        Map<Path, Boolean> deferredDirectories = new HashMap<>();
        int deferredFiles = 0;
        long deferredBytes = 0;
        Snapshot snapshot = new Snapshot(files, new File(BACKUP_TEMP_FOLDER, "snapshot/world-data.bin"));
        try {
            Files.createDirectories(snapshot.spool.toPath().getParent());
            byte[] buffer = new byte[64 * 1024];
            try (OutputStream output = new BufferedOutputStream(
                    Files.newOutputStream(snapshot.spool.toPath()),
                    buffer.length)) {
                for (Map.Entry<String, File> entry : files.entrySet()) {
                    File file = entry.getValue();
                    if (isWorldRegionFile(file, world)) continue;
                    // Reuse the walk's attributes; exact-length copying still rejects files that grow or shrink.
                    BasicFileAttributes attributes = listedAttributes.get(file);
                    if (canDeferWorldData(file, world, realWorld, attributes, deferredDirectories)) {
                        deferredFiles++;
                        deferredBytes += attributes.size();
                        continue;
                    }
                    if (attributes.isSymbolicLink() || attributes.isOther()) {
                        attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                    }
                    ZipEntry captured = new ZipEntry(entry.getKey());
                    captured.setSize(attributes.size());
                    captured.setTime(attributes.lastModifiedTime().toMillis());
                    try (CheckedInputStream input = new CheckedInputStream(new FileInputStream(file), new CRC32())) {
                        ICompress.copyExactly(input, output, captured.getSize(), buffer);
                        if (input.read() != -1) throw new IOException("File grew during backup snapshot: " + file);
                        captured.setCrc(input.getChecksum().getValue());
                    }
                    snapshot.entries.add(captured);
                }
            }
            ServerUtilities.LOGGER.info(
                    "Backup snapshot: {} files listed in {} ms; {} non-region files spooled in {} ms ({} bytes); {} stable files deferred to worker ({} bytes)",
                    files.size(),
                    (listed - started) / 1_000_000L,
                    snapshot.entries.size(),
                    (System.nanoTime() - listed) / 1_000_000L,
                    Files.size(snapshot.spool.toPath()),
                    deferredFiles,
                    deferredBytes);
            return snapshot;
        } catch (IOException | RuntimeException ex) {
            deleteSnapshot();
            throw ex;
        }
    }

    private static int compressSnapshot(Snapshot snapshot, Map<String, File> files, ICompress compressor)
            throws IOException {
        int index = 0;
        int total = files.size();
        try (InputStream input = new BufferedInputStream(new FileInputStream(snapshot.spool), 64 * 1024)) {
            for (ZipEntry entry : snapshot.entries) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Backup cancelled");
                logProgress(index++, total, entry.getName());
                compressor.addStreamToArchive(input, new ZipEntry(entry));
                files.remove(entry.getName());
            }
            if (input.read() != -1) throw new IOException("Unexpected trailing data in backup snapshot");
        }
        return index;
    }

    static void deleteSnapshot() {
        FileUtils.delete(new File(BACKUP_TEMP_FOLDER, "snapshot"));
    }

    private static Map<String, File> listWorldFiles(File src, Map<File, BasicFileAttributes> attributes)
            throws IOException {
        Map<String, File> files = new LinkedHashMap<>();
        Map<File, BasicFileAttributes> listed = listOutsideBackupStorage(src);
        if (attributes != null) attributes.putAll(listed);
        for (File file : listed.keySet()) {
            files.put(FileUtils.getRelativePath(file), file);
        }
        for (String name : new String[] { "ranks.txt", "players.txt" }) {
            File file = new File(ServerUtilities.SERVER_FOLDER, name);
            if (file.isFile()) {
                files.put(FileUtils.getRelativePath(file), file);
                if (attributes != null) {
                    attributes.put(file, Files.readAttributes(file.toPath(), BasicFileAttributes.class));
                }
            }
        }
        return files;
    }

    public static boolean isBackupStorage(File file) throws IOException {
        return isBackupStorage(
                file,
                FileUtils.resolveRealPath(BACKUP_TEMP_FOLDER.toPath()),
                FileUtils.resolveRealPath(BackupTask.BACKUP_FOLDER.toPath()));
    }

    /**
     * Lists files below {@code root}, pruning backup output and staging directories as the walk descends.
     * Canonicalizing every file instead costs about a millisecond each on Windows, where modern JDKs no longer cache
     * canonical paths, and this runs on the server thread while world saving is suspended.
     */
    private static Map<File, BasicFileAttributes> listOutsideBackupStorage(File root) throws IOException {
        return listOutsideBackupStorage(root, path -> true, path -> true);
    }

    private static Map<File, BasicFileAttributes> listOutsideBackupStorage(File root, PathMatcher selection,
            PathMatcher traversal) throws IOException {
        Path temp = FileUtils.resolveRealPath(BACKUP_TEMP_FOLDER.toPath());
        Path output = FileUtils.resolveRealPath(BackupTask.BACKUP_FOLDER.toPath());
        Map<File, BasicFileAttributes> files = new LinkedHashMap<>();
        // Missing optional include paths are normal; inaccessible paths must still reach the checked read below.
        if (Files.notExists(root.toPath())) return files;
        Path rootPath = root.toPath().normalize();
        if (!isBackupStorage(root, temp, output)) collectOutsideBackupStorage(
                files,
                root,
                temp,
                output,
                selection,
                path -> path.normalize().equals(rootPath) || traversal.matches(path));
        return files;
    }

    static void collectOutsideBackupStorage(Map<File, BasicFileAttributes> files, File file, Path temp, Path output,
            PathMatcher selection, PathMatcher traversal) throws IOException {
        Path path = file.toPath();
        boolean descend = traversal.matches(path);
        if (!descend && !selection.matches(path.normalize())) return;
        BasicFileAttributes attributes = Files
                .readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        // Windows junctions may be reported as other reparse points rather than symbolic links.
        boolean link = attributes.isSymbolicLink() || attributes.isOther();
        BasicFileAttributes linkAttributes = attributes;
        if (link) attributes = Files.readAttributes(path, BasicFileAttributes.class);
        if (attributes.isDirectory() && !descend) return;
        // Directories and links can bring backup storage into the walk; ordinary files below a checked directory
        // cannot.
        if ((attributes.isDirectory() || link) && isBackupStorage(file, temp, output)) return;
        if (!attributes.isDirectory()) {
            // Preserve link identity so snapshot deferral never treats a link as stable mod data.
            if (attributes.isRegularFile() && selection.matches(path.normalize())) files.put(file, linkAttributes);
            return;
        }
        File[] children = file.listFiles();
        if (children == null) throw new IOException("Cannot list backup directory: " + file);
        for (File child : children) {
            collectOutsideBackupStorage(files, child, temp, output, selection, traversal);
        }
    }

    private static boolean isBackupStorage(File file, Path temp, Path output) throws IOException {
        Path path = FileUtils.resolveRealPath(file.toPath());
        return path.startsWith(temp) || path.startsWith(output);
    }

    private static void validateBackupSource(File src) throws IOException {
        if (isBackupStorage(src)) {
            throw new IOException("Backup and temporary storage must not contain the world directory: " + src);
        }
        if (!Files.readAttributes(src.toPath(), BasicFileAttributes.class).isDirectory()) {
            throw new IOException("World backup source is not a directory: " + src);
        }
    }

    private static boolean isWorldRegionFile(File file, Path world) {
        return file.getName().endsWith(".mca") && file.toPath().toAbsolutePath().normalize().startsWith(world);
    }

    private static boolean canDeferWorldData(File file, Path world, Path realWorld, BasicFileAttributes attributes,
            Map<Path, Boolean> directories) throws IOException {
        if (!attributes.isRegularFile()) return false;
        Path path = file.toPath().toAbsolutePath().normalize();
        if (!path.startsWith(world)) return false;
        Path relative = world.relativize(path);
        String name = file.getName();
        // AE2 flushes meteor spawn data on world saves and shutdown, not on ordinary gameplay updates.
        // World saving stays suspended, and the shutdown hook joins the worker before mod stopping events.
        boolean stable = relative.getNameCount() == 3 && relative.startsWith(Paths.get("AE2", "spawndata"))
                && name.endsWith(".dat");
        // BQ's versioned migration copies are written during database loading. Live BQ files remain captured.
        if (relative.getNameCount() == 4 && relative.startsWith(Paths.get("betterquesting", "backup"))) {
            String suffix = "_backup_" + relative.getName(2) + ".json";
            for (String database : new String[] { "QuestDatabase", "QuestProgress", "QuestingParties", "NameCache",
                    "LifeDatabase" }) {
                if (name.equals(database + suffix)) stable = true;
            }
        }
        if (!stable) return false;
        Path parent = path.getParent();
        Boolean ordinaryDirectory = directories.get(parent);
        if (ordinaryDirectory == null) {
            // Check once per directory, including its ancestors, without canonicalizing thousands of files.
            ordinaryDirectory = parent.toRealPath().equals(realWorld.resolve(world.relativize(parent)));
            directories.put(parent, ordinaryDirectory);
        }
        return ordinaryDirectory;
    }

    private static void logProgress(int i, int allFiles, String name) {
        long millis = System.currentTimeMillis();
        boolean first = i == 0;
        if (first) {
            ServerUtilities.LOGGER.info("Backing up {} files...", allFiles);
        }

        if (first || millis > logMillis || i == allFiles - 1) {
            logMillis = millis + Ticks.SECOND.x(5).millis();
            ServerUtilities.LOGGER
                    .info("[{} | {}%]: {}", i, StringUtils.formatDouble00((i / (double) allFiles) * 100D), name);
        }
    }

    private static void compressFiles(Map<String, File> files, ICompress compressor, int captured) throws IOException {
        int allFiles = files.size() + captured;
        int index = captured;
        for (Map.Entry<String, File> entry : files.entrySet()) {
            compressFile(entry.getKey(), entry.getValue(), compressor, index++, allFiles);
        }
    }

    private static void compressFile(String entryName, File file, ICompress compressor, int index, int totalFiles)
            throws IOException {
        // interrupt() cannot abort java.io reads, so the backup has to bail out cooperatively
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Backup cancelled");
        logProgress(index, totalFiles, file.getAbsolutePath());
        compressor.addFileToArchive(file, entryName);
    }

    private static void backupRegions(Map<String, File> files, File src, Set<ChunkDimPos> chunksToBackup,
            ICompress compressor, Map<Integer, File> dimensionFolders, int captured) throws IOException {
        Object2ObjectMap<File, ObjectSet<ChunkDimPos>> dimRegionClaims = mapClaimsToRegionFile(
                chunksToBackup,
                dimensionFolders);
        Path world = src.toPath().toAbsolutePath().normalize();
        Set<Path> regionFolders = new HashSet<>();
        for (File folder : dimensionFolders.values()) {
            regionFolders.add(new File(folder, "region").toPath().toAbsolutePath().normalize());
        }
        Set<Path> unknownFolders = new HashSet<>();
        files.entrySet().removeIf(entry -> {
            File file = entry.getValue();
            if (!isWorldRegionFile(file, world)) return false;
            Path parent = file.toPath().toAbsolutePath().normalize().getParent();
            if (regionFolders.contains(parent)) return true;
            unknownFolders.add(parent);
            return false;
        });
        for (Path folder : unknownFolders) {
            ServerUtilities.LOGGER
                    .warn("Cannot identify dimension for region files in {}; copying them unchanged", folder);
        }

        int index = captured;
        int savedChunks = 0;
        int regionFiles = dimRegionClaims.size();
        int totalFiles = files.size() + regionFiles + captured;

        if (backups.backup_entire_regions_with_claims) {
            // Backup entire region files that contain claimed chunks
            for (Object2ObjectMap.Entry<File, ObjectSet<ChunkDimPos>> entry : dimRegionClaims.object2ObjectEntrySet()) {
                File regionFile = entry.getKey();
                ObjectSet<ChunkDimPos> claimedChunks = entry.getValue();
                savedChunks += claimedChunks.size();

                // Backup the entire region file as-is
                compressFile(FileUtils.getRelativePath(regionFile), regionFile, compressor, index++, totalFiles);
            }
            ServerUtilities.LOGGER
                    .info("Backed up {} entire regions containing {} claimed chunks", regionFiles, savedChunks);
        } else {
            // Standard behavior: reconstruct temporary region files with only claimed chunks
            for (Object2ObjectMap.Entry<File, ObjectSet<ChunkDimPos>> entry : dimRegionClaims.object2ObjectEntrySet()) {
                File file = entry.getKey();
                long size = Files.size(file.toPath());
                if (size < 8192 || size % 4096 != 0) {
                    // RegionFile opens read-write and pads malformed files in its constructor.
                    ServerUtilities.LOGGER.warn("Cannot trim malformed region {}; copying it unchanged", file);
                    compressFile(FileUtils.getRelativePath(file), file, compressor, index++, totalFiles);
                    continue;
                }
                Files.createDirectories(BACKUP_TEMP_FOLDER.toPath());
                File tempFile = Files.createTempFile(BACKUP_TEMP_FOLDER.toPath(), "claimed-", ".mca").toFile();
                try {
                    RegionFile sourceRegion = new RegionFile(file);
                    RegionFile tempRegion = new RegionFile(tempFile);
                    boolean hasData = false;
                    try {
                        for (ChunkDimPos pos : entry.getValue()) {
                            if (Thread.currentThread().isInterrupted())
                                throw new InterruptedIOException("Backup cancelled");
                            try (DataInputStream in = sourceRegion
                                    .getChunkDataInputStream(pos.posX & 31, pos.posZ & 31)) {
                                if (in == null) continue;
                                NBTTagCompound tag = CompressedStreamTools.read(in);
                                try (DataOutputStream out = tempRegion
                                        .getChunkDataOutputStream(pos.posX & 31, pos.posZ & 31)) {
                                    CompressedStreamTools.write(tag, out);
                                }
                                savedChunks++;
                                hasData = true;
                            }
                        }
                    } finally {
                        try {
                            sourceRegion.close();
                        } finally {
                            tempRegion.close();
                        }
                    }
                    if (hasData) {
                        compressFile(FileUtils.getRelativePath(file), tempFile, compressor, index++, totalFiles);
                    }
                } finally {
                    Files.deleteIfExists(tempFile.toPath());
                }
            }
            ServerUtilities.LOGGER.info("Backed up {} regions containing {} claimed chunks", regionFiles, savedChunks);
        }

        for (Map.Entry<String, File> entry : files.entrySet()) {
            compressFile(entry.getKey(), entry.getValue(), compressor, index++, totalFiles);
        }
    }

    private static Map<Integer, File> resolveDimensionFolders(File src) {
        Map<Integer, File> folders = new HashMap<>();
        for (WorldServer world : ServerUtils.getServer().worldServers) {
            if (world != null) folders.put(world.provider.dimensionId, world.getChunkSaveLocation());
        }
        // Include dimensions with no claims so their unclaimed regions are still filtered out.
        for (int dimension : DimensionManager.getStaticDimensionIDs()) {
            folders.computeIfAbsent(dimension, dim -> {
                try {
                    String folder = DimensionManager.createProviderFor(dim).getSaveFolder();
                    return folder == null ? src : new File(src, folder);
                } catch (RuntimeException e) {
                    // Leave it unresolved rather than failing every claim-only backup; its regions are kept whole.
                    ServerUtilities.LOGGER
                            .warn("Cannot resolve save folder for dimension {}; keeping its regions unchanged", dim, e);
                    return null;
                }
            });
        }
        return folders;
    }

    private static Object2ObjectMap<File, ObjectSet<ChunkDimPos>> mapClaimsToRegionFile(Set<ChunkDimPos> chunks,
            Map<Integer, File> dimensionFolders) throws IOException {
        Object2ObjectMap<File, ObjectSet<ChunkDimPos>> regions = new Object2ObjectOpenHashMap<>();
        for (ChunkDimPos pos : chunks) {
            File folder = dimensionFolders.get(pos.dim);
            // Removed dimensions have no reliable path; their unknown region folders are kept unchanged.
            if (folder == null) continue;
            File file = new File(folder, "region/r." + (pos.posX >> 5) + "." + (pos.posZ >> 5) + ".mca");
            regions.computeIfAbsent(file, key -> new ObjectOpenHashSet<>()).add(pos);
        }
        for (java.util.Iterator<File> it = regions.keySet().iterator(); it.hasNext();) {
            File file = it.next();
            if (Files.notExists(file.toPath())) it.remove();
            else if (!file.isFile()) throw new IOException("Cannot read claimed region file " + file);
        }
        return regions;
    }

    private static String getDoneTime(long l) {
        return StringUtils.getTimeString(System.currentTimeMillis() - l);
    }

}
