package serverutils.task.backup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import serverutils.lib.math.ChunkDimPos;

public class ThreadBackup extends Thread {

    private static boolean isWorldRegionFile(File file, Path world) {
        return file.getName().endsWith(".mca") && file.toPath().toAbsolutePath().normalize().startsWith(world);
    }

    // private static void backupRegions(Map<String, File> files, File src, Set<ChunkDimPos> chunksToBackup,
    // ICompress compressor, Map<Integer, File> dimensionFolders, int captured) throws IOException {
    // Object2ObjectMap<File, ObjectSet<ChunkDimPos>> dimRegionClaims = mapClaimsToRegionFile(
    // chunksToBackup,
    // dimensionFolders);
    // Path world = src.toPath().toAbsolutePath().normalize();
    // Set<Path> regionFolders = new HashSet<>();
    // for (File folder : dimensionFolders.values()) {
    // regionFolders.add(new File(folder, "region").toPath().toAbsolutePath().normalize());
    // }
    // Set<Path> unknownFolders = new HashSet<>();
    // files.entrySet().removeIf(entry -> {
    // File file = entry.getValue();
    // if (!isWorldRegionFile(file, world)) return false;
    // Path parent = file.toPath().toAbsolutePath().normalize().getParent();
    // if (regionFolders.contains(parent)) return true;
    // unknownFolders.add(parent);
    // return false;
    // });
    // for (Path folder : unknownFolders) {
    // ServerUtilities.LOGGER
    // .warn("Cannot identify dimension for region files in {}; copying them unchanged", folder);
    // }
    //
    // int index = captured;
    // int savedChunks = 0;
    // int regionFiles = dimRegionClaims.size();
    // int totalFiles = files.size() + regionFiles + captured;
    //
    // if (backups.backup_entire_regions_with_claims) {
    // // Backup entire region files that contain claimed chunks
    // for (Object2ObjectMap.Entry<File, ObjectSet<ChunkDimPos>> entry : dimRegionClaims.object2ObjectEntrySet()) {
    // File regionFile = entry.getKey();
    // ObjectSet<ChunkDimPos> claimedChunks = entry.getValue();
    // savedChunks += claimedChunks.size();
    //
    // // Backup the entire region file as-is
    // compressFile(FileUtils.getRelativePath(regionFile), regionFile, compressor, index++, totalFiles);
    // }
    // ServerUtilities.LOGGER
    // .info("Backed up {} entire regions containing {} claimed chunks", regionFiles, savedChunks);
    // } else {
    // // Standard behavior: reconstruct temporary region files with only claimed chunks
    // for (Object2ObjectMap.Entry<File, ObjectSet<ChunkDimPos>> entry : dimRegionClaims.object2ObjectEntrySet()) {
    // File file = entry.getKey();
    // long size = Files.size(file.toPath());
    // if (size < 8192 || size % 4096 != 0) {
    // // RegionFile opens read-write and pads malformed files in its constructor.
    // ServerUtilities.LOGGER.warn("Cannot trim malformed region {}; copying it unchanged", file);
    // compressFile(FileUtils.getRelativePath(file), file, compressor, index++, totalFiles);
    // continue;
    // }
    // Files.createDirectories(BACKUP_TEMP_FOLDER.toPath());
    // File tempFile = Files.createTempFile(BACKUP_TEMP_FOLDER.toPath(), "claimed-", ".mca").toFile();
    // try {
    // RegionFile sourceRegion = new RegionFile(file);
    // RegionFile tempRegion = new RegionFile(tempFile);
    // boolean hasData = false;
    // try {
    // for (ChunkDimPos pos : entry.getValue()) {
    // if (Thread.currentThread().isInterrupted())
    // throw new InterruptedIOException("Backup cancelled");
    // try (DataInputStream in = sourceRegion
    // .getChunkDataInputStream(pos.posX & 31, pos.posZ & 31)) {
    // if (in == null) continue;
    // NBTTagCompound tag = CompressedStreamTools.read(in);
    // try (DataOutputStream out = tempRegion
    // .getChunkDataOutputStream(pos.posX & 31, pos.posZ & 31)) {
    // CompressedStreamTools.write(tag, out);
    // }
    // savedChunks++;
    // hasData = true;
    // }
    // }
    // } finally {
    // try {
    // sourceRegion.close();
    // } finally {
    // tempRegion.close();
    // }
    // }
    // if (hasData) {
    // compressFile(FileUtils.getRelativePath(file), tempFile, compressor, index++, totalFiles);
    // }
    // } finally {
    // Files.deleteIfExists(tempFile.toPath());
    // }
    // }
    // ServerUtilities.LOGGER.info("Backed up {} regions containing {} claimed chunks", regionFiles, savedChunks);
    // }
    //
    // for (Map.Entry<String, File> entry : files.entrySet()) {
    // compressFile(entry.getKey(), entry.getValue(), compressor, index++, totalFiles);
    // }
    // }

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
}
