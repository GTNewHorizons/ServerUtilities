package serverutils.task.backup;

import static serverutils.ServerUtilitiesConfig.backups;
import static serverutils.ServerUtilitiesNotifications.BACKUP_END1;
import static serverutils.ServerUtilitiesNotifications.BACKUP_END2;
import static serverutils.task.backup.BackupTask.BACKUP_TEMP_FOLDER;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
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
import serverutils.ServerUtilitiesNotifications;
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
    public boolean isDone = false;
    private final ICompress compressor;

    public ThreadBackup(ICompress compress, File sourceFile, String backupName, Set<ChunkDimPos> backupChunks) {
        src0 = sourceFile;
        customName = backupName;
        chunksToBackup = backupChunks;
        compressor = compress;
        setPriority(7);
    }

    public void run() {
        isDone = false;
        doBackup(compressor, src0, customName, chunksToBackup);
        isDone = true;
    }

    public static void doBackup(ICompress compressor, File src, String customName, Set<ChunkDimPos> chunks) {
        String outName = (customName.isEmpty() ? DATE_FORMAT.format(Calendar.getInstance().getTime()) : customName)
                + ".zip";
        File dstFile = null;
        try {
            List<File> files = FileUtils.listTree(src);
            long start = System.currentTimeMillis();
            logMillis = start + Ticks.SECOND.x(5).millis();

            dstFile = FileUtils.newFile(new File(BackupTask.backupsFolder, outName));
            try (compressor) {
                compressor.createOutputStream(dstFile);
                if (!chunks.isEmpty() && backups.only_backup_claimed_chunks) {
                    backupRegions(src, files, chunks, compressor);
                } else {
                    compressFiles(src, files, compressor);
                }

                String backupSize = FileUtils.getSizeString(dstFile);
                ServerUtilities.LOGGER.info("Backup done in {} seconds ({})!", getDoneTime(start), backupSize);
                ServerUtilities.LOGGER.info("Created {} from {}", dstFile.getAbsolutePath(), src.getAbsolutePath());

                if (backups.display_file_size) {
                    String sizeT = FileUtils.getSizeString(BackupTask.backupsFolder);
                    ServerUtilitiesNotifications.backupNotification(
                            BACKUP_END2,
                            "cmd.backup_end_2",
                            getDoneTime(start),
                            (backupSize.equals(sizeT) ? backupSize : (backupSize + " | " + sizeT)));
                } else {
                    ServerUtilitiesNotifications
                            .backupNotification(BACKUP_END1, "cmd.backup_end_1", getDoneTime(start));
                }
            }
        } catch (Exception e) {
            IChatComponent c = StringUtils
                    .color(ServerUtilities.lang(null, "cmd.backup_fail", e), EnumChatFormatting.RED);
            ServerUtils.notifyChat(ServerUtils.getServer(), null, c);
            ServerUtilities.LOGGER.error("Error while backing up", e);

            if (dstFile != null) FileUtils.delete(dstFile);
        }
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

    private static void compressFiles(File sourceDir, List<File> files, ICompress compressor) throws IOException {
        int allFiles = files.size();
        for (int i = 0; i < allFiles; i++) {
            File file = files.get(i);
            compressFile(FileUtils.getRelativePath(sourceDir, file), file, compressor, i, allFiles);
        }
    }

    private static void compressFile(String entryName, File file, ICompress compressor, int index, int totalFiles)
            throws IOException {
        logProgress(index, totalFiles, file.getAbsolutePath());
        compressor.addFileToArchive(file, entryName);
    }

    private static void backupRegions(File sourceFolder, List<File> files, Set<ChunkDimPos> chunksToBackup,
            ICompress compressor) throws IOException {
        Object2ObjectMap<File, ObjectSet<ChunkDimPos>> dimRegionClaims = mapClaimsToRegionFile(chunksToBackup);
        files.removeIf(f -> f.getName().endsWith(".mca"));

        int index = 0;
        int savedChunks = 0;
        int regionFiles = dimRegionClaims.size();
        int totalFiles = files.size() + regionFiles;
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
                compressFile(FileUtils.getRelativePath(sourceFolder, file), tempFile, compressor, index++, totalFiles);
            }

            FileUtils.delete(tempFile);
        }

        for (File file : files) {
            compressFile(FileUtils.getRelativePath(sourceFolder, file), file, compressor, index++, totalFiles);
        }

        ServerUtilities.LOGGER.info("Backed up {} regions containing {} claimed chunks", regionFiles, savedChunks);
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

    private static int[] getRegionCoords(File f) {
        String fileName = f.getName();
        int firstDot = fileName.indexOf('.');
        int secondDot = fileName.indexOf('.', firstDot + 1);

        int x = Integer.parseInt(fileName.substring(firstDot + 1, secondDot));
        int z = Integer.parseInt(fileName.substring(secondDot + 1, fileName.lastIndexOf('.')));
        return new int[] { x, z };
    }

    private static String getDoneTime(long l) {
        return StringUtils.getTimeString(System.currentTimeMillis() - l);
    }

    private static long getRegionFromChunk(int chunkX, int chunkZ) {
        return CoordinatePacker.pack(chunkX >> 5, 0, chunkZ >> 5);
    }
}
