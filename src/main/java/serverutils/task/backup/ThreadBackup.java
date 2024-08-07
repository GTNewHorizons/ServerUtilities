package serverutils.task.backup;

import static serverutils.ServerUtilitiesConfig.backups;
import static serverutils.ServerUtilitiesNotifications.BACKUP_END1;
import static serverutils.ServerUtilitiesNotifications.BACKUP_END2;
import static serverutils.task.backup.BackupTask.BACKUP_TEMP_FOLDER;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.zip.ZipEntry;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.storage.RegionFile;
import net.minecraft.world.chunk.storage.RegionFileCache;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesNotifications;
import serverutils.data.ClaimedChunks;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.FileUtils;
import serverutils.lib.util.ServerUtils;
import serverutils.lib.util.StringUtils;

public class ThreadBackup extends Thread {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    private static long logMillis;
    private final File src0;
    private final String customName;
    public boolean isDone = false;

    public ThreadBackup(File w, String s) {
        src0 = w;
        customName = s;
        setPriority(7);
    }

    public void run() {
        isDone = false;
        doBackup(src0, customName);
        isDone = true;
    }

    public static void doBackup(File src, String customName) {
        String outName = (customName.isEmpty() ? DATE_FORMAT.format(Calendar.getInstance().getTime()) : customName)
                + ".zip";
        File dstFile = null;
        try {
            List<File> files = FileUtils.listTree(src);
            long start = System.currentTimeMillis();
            logMillis = start + Ticks.SECOND.x(5).millis();

            dstFile = FileUtils.newFile(new File(BackupTask.backupsFolder, outName));
            try (ZipArchiveOutputStream zaos = new ZipArchiveOutputStream(dstFile)) {
                if (backups.compression_level == 0) {
                    zaos.setMethod(ZipEntry.STORED);
                } else {
                    zaos.setLevel(backups.compression_level);
                }

                if (ClaimedChunks.isActive() && backups.only_backup_claimed_chunks) {
                    backupRegions(src, files, zaos);
                } else {
                    compressFiles(src, files, zaos);
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

    private static void compressFiles(File sourceDir, List<File> files, ZipArchiveOutputStream zaos)
            throws IOException {
        int allFiles = files.size();
        for (int i = 0; i < allFiles; i++) {
            File file = files.get(i);
            compressFile(FileUtils.getRelativePath(sourceDir, file), file, zaos, i, allFiles);
        }
    }

    private static void compressFile(String entryName, File file, ZipArchiveOutputStream out, int index, int totalFiles)
            throws IOException {
        ArchiveEntry entry = new ZipArchiveEntry(file, entryName);
        logProgress(index, totalFiles, file.getAbsolutePath());
        out.putArchiveEntry(entry);
        try (FileInputStream fis = new FileInputStream(file)) {
            IOUtils.copy(fis, out);
        }
        out.closeArchiveEntry();

    }

    private static void backupRegions(File sourceFolder, List<File> files, ZipArchiveOutputStream out)
            throws IOException {
        Long2ObjectMap<LongSet> claimedChunks = mapClaimsToRegion();
        Long2ObjectMap<File> regionFiles = mapRegionFilesToLong(claimedChunks);
        files.removeIf(f -> f.getName().endsWith(".mca"));

        int index = 0;
        int savedChunks = 0;
        int totalFiles = regionFiles.size() + files.size();
        ChunkDimPos mutableTemp = new ChunkDimPos();
        for (Long2ObjectMap.Entry<File> entry : regionFiles.long2ObjectEntrySet()) {
            File file = entry.getValue();
            LongSet chunks = claimedChunks.get(entry.getLongKey());
            if (chunks == null || chunks.isEmpty()) continue;
            File dimensionRoot = file.getParentFile().getParentFile();

            File tempFile = FileUtils.newFile(new File(BACKUP_TEMP_FOLDER, file.getName()));
            RegionFile tempRegion = new RegionFile(tempFile);
            boolean hasData = false;
            for (long pos : chunks) {
                mutableTemp.setFromLong(pos);
                DataInputStream in = RegionFileCache.getChunkInputStream(dimensionRoot, mutableTemp.x, mutableTemp.z);

                if (in == null) continue;
                savedChunks++;
                hasData = true;
                NBTTagCompound tag = CompressedStreamTools.read(in);
                DataOutputStream tempOut = tempRegion.getChunkDataOutputStream(mutableTemp.x & 31, mutableTemp.z & 31);
                CompressedStreamTools.write(tag, tempOut);
                tempOut.close();
            }

            tempRegion.close();
            if (hasData) {
                compressFile(FileUtils.getRelativePath(sourceFolder, file), tempFile, out, index++, totalFiles);
            }

            FileUtils.delete(tempFile);
        }

        for (File file : files) {
            compressFile(FileUtils.getRelativePath(sourceFolder, file), file, out, index++, totalFiles);
        }

        ServerUtilities.LOGGER
                .info("Backed up {} regions containing {} claimed chunks", regionFiles.size(), savedChunks);
    }

    private static Long2ObjectMap<File> mapRegionFilesToLong(Long2ObjectMap<LongSet> regionClaims) {
        Long2ObjectMap<File> regionFiles = new Long2ObjectOpenHashMap<>();
        MinecraftServer server = ServerUtils.getServer();
        for (WorldServer worldserver : server.worldServers) {
            if (worldserver == null) continue;

            int dim = worldserver.provider.dimensionId;
            File regionFolder = new File(worldserver.getChunkSaveLocation(), "region");
            if (!regionFolder.exists()) continue;

            File[] regions = regionFolder.listFiles();
            if (regions == null) continue;

            for (File file : regions) {
                int[] coords = getRegionCoords(file);
                long key = CoordinatePacker.pack(coords[0], dim, coords[1]);
                if (!regionClaims.containsKey(key)) {
                    ServerUtilities.LOGGER.info("Skipping region file: {}", file.getName());
                    continue;
                }
                regionFiles.put(key, file);
            }
        }
        return regionFiles;
    }

    private static Long2ObjectMap<LongSet> mapClaimsToRegion() {
        Long2ObjectMap<LongSet> regionClaims = new Long2ObjectOpenHashMap<>();
        ClaimedChunks.instance.getAllClaimedPositions().forEach(pos -> {
            long region = pos.toRegionLong();
            regionClaims.computeIfAbsent(region, k -> new LongOpenHashSet()).add(pos.toLong());
        });
        return regionClaims;
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
}
