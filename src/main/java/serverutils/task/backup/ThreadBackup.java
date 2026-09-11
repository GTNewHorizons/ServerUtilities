package serverutils.task.backup;

import static serverutils.ServerUtilitiesConfig.backups;
import static serverutils.ServerUtilitiesNotifications.BACKUP;
import static serverutils.task.backup.BackupTask.BACKUP_TEMP_FOLDER;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.storage.RegionFile;
import net.minecraft.world.chunk.storage.RegionFileCache;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
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
    private final Map<String, File> files;

    public ThreadBackup(ICompress compress, File sourceFile, String backupName, Set<ChunkDimPos> backupChunks) {
        this(compress, sourceFile, backupName, backupChunks, null);
    }

    ThreadBackup(ICompress compress, File sourceFile, String backupName, Set<ChunkDimPos> backupChunks,
            Map<String, File> snapshot) {
        src0 = sourceFile;
        customName = backupName;
        chunksToBackup = backupChunks;
        compressor = compress;
        files = snapshot;
        setPriority(7);
    }

    public void run() {
        try {
            doBackup(compressor, src0, customName, chunksToBackup, files);
        } finally {
            if (files != null) deleteSnapshot();
        }
    }

    private static void addBaseFolderFiles(Map<String, File> files, File saveFile) {
        String saveName = saveFile.getName();

        for (String pattern : backups.additional_backup_files) {
            pattern = pattern.replace("$WORLDNAME", saveName);

            int firstWildcardIndex = pattern.indexOf('*');
            if (firstWildcardIndex == -1) {
                for (File file : FileUtils.listTree(new File(pattern))) {
                    files.putIfAbsent(FileUtils.getRelativePath(file), file);
                }
                continue;
            }

            Path rootFolder = Paths.get(pattern.substring(0, firstWildcardIndex));

            // If wildcard was not at the start of a directory, get the parent
            if (firstWildcardIndex != 0 && (pattern.charAt(firstWildcardIndex - 1) != '/')) {
                rootFolder = rootFolder.getParent();
            }

            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            List<File> fileCandidates = FileUtils.listTree(rootFolder.toFile());
            for (File file : fileCandidates) {
                if (matcher.matches(file.toPath())) {
                    files.putIfAbsent(FileUtils.getRelativePath(file), file);
                }
            }
        }
    }

    public static void doBackup(ICompress compressor, File src, String customName, Set<ChunkDimPos> chunks) {
        doBackup(compressor, src, customName, chunks, null);
    }

    static void doBackup(ICompress compressor, File src, String customName, Set<ChunkDimPos> chunks,
            Map<String, File> files) {
        String outName = (customName.isEmpty() ? DATE_FORMAT.format(Calendar.getInstance().getTime()) : customName)
                + ".zip";
        File dstFile = null;
        try {
            if (files == null) files = listWorldFiles(src);
            addBaseFolderFiles(files, src);
            long start = System.currentTimeMillis();
            logMillis = start + Ticks.SECOND.x(5).millis();

            dstFile = FileUtils.newFile(new File(BackupTask.BACKUP_FOLDER, outName));
            try (compressor) {
                compressor.createOutputStream(dstFile);
                if (!chunks.isEmpty() && backups.only_backup_claimed_chunks) {
                    backupRegions(files, src, chunks, compressor);
                } else {
                    compressFiles(files, compressor);
                }

            }
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
            if (dstFile != null) FileUtils.delete(dstFile);
        } catch (Exception e) {
            ServerUtils.notifyChat(
                    ServerUtils.getServer(),
                    null,
                    StringUtils.color("cmd.backup_fail", EnumChatFormatting.RED, e.getMessage()));
            ServerUtilities.LOGGER.error("Error while backing up", e);

            if (dstFile != null) FileUtils.delete(dstFile);
        }
    }

    static Map<String, File> snapshotFiles(File src) throws IOException {
        deleteSnapshot();
        if (!BACKUP_TEMP_FOLDER.mkdirs() && !BACKUP_TEMP_FOLDER.isDirectory()) {
            throw new IOException("Could not create backup staging directory");
        }

        Map<String, File> files = listWorldFiles(src);
        Path world = src.toPath().toAbsolutePath().normalize();
        int index = 0;
        try {
            for (Map.Entry<String, File> entry : files.entrySet()) {
                File file = entry.getValue();
                if (isWorldRegionFile(file, world)) continue;

                File copy = new File(BACKUP_TEMP_FOLDER, "snapshot/" + index++);
                Files.createDirectories(copy.toPath().getParent());
                Files.copy(
                        file.toPath(),
                        copy.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES);
                entry.setValue(copy);
            }
            return files;
        } catch (IOException | RuntimeException ex) {
            deleteSnapshot();
            throw ex;
        }
    }

    static void deleteSnapshot() {
        FileUtils.delete(new File(BACKUP_TEMP_FOLDER, "snapshot"));
    }

    private static Map<String, File> listWorldFiles(File src) {
        Map<String, File> files = new LinkedHashMap<>();
        for (File file : FileUtils.listTree(src)) {
            files.put(FileUtils.getRelativePath(file), file);
        }
        for (String name : new String[] { "ranks.txt", "players.txt" }) {
            File file = new File(ServerUtilities.SERVER_FOLDER, name);
            if (file.isFile()) files.put(FileUtils.getRelativePath(file), file);
        }
        return files;
    }

    private static boolean isWorldRegionFile(File file, Path world) {
        return file.getName().endsWith(".mca") && file.toPath().toAbsolutePath().normalize().startsWith(world);
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

    private static void compressFiles(Map<String, File> files, ICompress compressor) throws IOException {
        int allFiles = files.size();
        int index = 0;
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
            ICompress compressor) throws IOException {
        Object2ObjectMap<File, ObjectSet<ChunkDimPos>> dimRegionClaims = mapClaimsToRegionFile(chunksToBackup);
        Path world = src.toPath().toAbsolutePath().normalize();
        files.entrySet().removeIf(entry -> isWorldRegionFile(entry.getValue(), world));

        int index = 0;
        int savedChunks = 0;
        int regionFiles = dimRegionClaims.size();
        int totalFiles = files.size() + regionFiles;

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
                File dimensionRoot = file.getParentFile().getParentFile();
                File tempFile = FileUtils.newFile(new File(BACKUP_TEMP_FOLDER, file.getName()));
                RegionFile tempRegion = new RegionFile(tempFile);
                boolean hasData = false;

                for (ChunkDimPos pos : entry.getValue()) {
                    DataInputStream in = RegionFileCache.getChunkInputStream(dimensionRoot, pos.posX, pos.posZ);
                    if (in == null) continue;
                    savedChunks++;
                    hasData = true;
                    NBTTagCompound tag = CompressedStreamTools.read(in);
                    DataOutputStream tempOut = tempRegion.getChunkDataOutputStream(pos.posX & 31, pos.posZ & 31);
                    CompressedStreamTools.write(tag, tempOut);
                    tempOut.close();
                }

                tempRegion.close();
                if (hasData) {
                    compressFile(FileUtils.getRelativePath(file), tempFile, compressor, index++, totalFiles);
                }

                FileUtils.delete(tempFile);
            }
            ServerUtilities.LOGGER.info("Backed up {} regions containing {} claimed chunks", regionFiles, savedChunks);
        }

        for (Map.Entry<String, File> entry : files.entrySet()) {
            compressFile(entry.getKey(), entry.getValue(), compressor, index++, totalFiles);
        }
    }

    private static Object2ObjectMap<File, ObjectSet<ChunkDimPos>> mapClaimsToRegionFile(
            Set<ChunkDimPos> chunksToBackup) {
        Int2ObjectMap<Long2ObjectMap<ObjectSet<ChunkDimPos>>> regionClaimsByDim = new Int2ObjectOpenHashMap<>();
        chunksToBackup.forEach(
                pos -> regionClaimsByDim.computeIfAbsent(pos.dim, k -> new Long2ObjectOpenHashMap<>())
                        .computeIfAbsent(getRegionFromChunk(pos.posX, pos.posZ), k -> new ObjectOpenHashSet<>())
                        .add(pos));

        Object2ObjectMap<File, ObjectSet<ChunkDimPos>> regionFilesToBackup = new Object2ObjectOpenHashMap<>();
        for (WorldServer worldserver : ServerUtils.getServer().worldServers) {
            if (worldserver == null) continue;

            int dim = worldserver.provider.dimensionId;
            File regionFolder = new File(worldserver.getChunkSaveLocation(), "region");
            Long2ObjectMap<ObjectSet<ChunkDimPos>> regionClaims = regionClaimsByDim.get(dim);
            if (!regionFolder.exists() || regionClaims == null) continue;

            File[] regions = regionFolder.listFiles();
            if (regions == null) continue;

            for (File file : regions) {
                int[] coords = getRegionCoords(file);
                if (coords == null) continue;
                long key = CoordinatePacker.pack(coords[0], 0, coords[1]);
                ObjectSet<ChunkDimPos> claims = regionClaims.get(key);
                if (claims == null) {
                    if (ServerUtilitiesConfig.debugging.print_more_info) {
                        ServerUtilities.LOGGER.info("Skipping region file {} from dimension {}", file.getName(), dim);
                    }
                    continue;
                }
                regionFilesToBackup.put(file, claims);
            }
        }
        return regionFilesToBackup;
    }

    private static int[] getRegionCoords(File file) {
        if (!file.getName().endsWith(".mca")) return null;

        String[] parts = file.getName().split("\\.");
        try {
            int x = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            return new int[] { x, z };
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    private static String getDoneTime(long l) {
        return StringUtils.getTimeString(System.currentTimeMillis() - l);
    }

    private static long getRegionFromChunk(int chunkX, int chunkZ) {
        return CoordinatePacker.pack(chunkX >> 5, 0, chunkZ >> 5);
    }
}
