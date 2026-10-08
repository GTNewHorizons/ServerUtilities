package serverutils.lib.util.backup;

import com.google.gson.Gson;
import cpw.mods.fml.common.Loader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import serverutils.ServerUtilities;

public class Snapshot {
    private static final Gson GSON = new Gson();
    // sortable and filename safe, so lexicographic order is chronological order
    private static final DateTimeFormatter NAME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss'Z'");
    private static final Pattern SNAPSHOT_FILE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}-\\d{2}-\\d{2}Z\\.json");

    private ZonedDateTime createdAt;
    private ZonedDateTime completedAt;
    private String minecraftVersion;
    private String modVersion;
    private String world;

    private Map<Integer, SnapshotDimension> dimensions;
    private Map<String, String> files;

    private Snapshot(String world) {
        this.createdAt = ZonedDateTime.now(ZoneOffset.UTC);
        this.minecraftVersion = Loader.instance().getMCVersionString();
        this.modVersion = ServerUtilities.VERSION;
        this.files = new HashMap<>();
        this.world = world;
    }

    private Snapshot(File root, SnapshotJson json) {
        this.createdAt = ZonedDateTime.parse(json.createdAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        this.completedAt = ZonedDateTime.parse(json.completedAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        this.minecraftVersion = json.minecraftVersion;
        this.modVersion = json.modVersion;
        this.dimensions = getDimensions(root, json);
    }

    public static Snapshot create(
        File source,
        File destination,
        ChunkReader reader
    ) {
        if (!source.isDirectory()) throw new IllegalArgumentException("Not a directory");

        Snapshot snapshot = new Snapshot(source.getName());
        snapshot.process(source, destination, reader);

        return snapshot;
    }

    private void process(File source, File destination, ChunkReader reader) {
        Snapshot lastSnapshot = findLatest(destination);

        this.dimensions = getDimensions(source);

        File objects = ensureSubDirectory(destination, "objects");
        File packs = ensureSubDirectory(objects, "packs");
        File manifests = ensureSubDirectory(objects, "manifests");

        Set<String> known;
        try {
            known = PackIndex.loadKnownHashes(packs);
        } catch (IOException e) {
            throw new SnapshotException("Failed to read existing pack indexes", e);
        }

        try (PackWriter packWriter = new PackWriter(packs, known)) {
            writeDimensions(manifests, packWriter, reader, lastSnapshot);
        } catch (IOException e) {
            throw new SnapshotException("Failed to write pack file", e);
        }

        this.completedAt = ZonedDateTime.now(ZoneOffset.UTC);

        SnapshotJson json = toJson();
        File snapshotFile = new File(destination, getName() + ".json");

        try {
            SnapshotJson.write(snapshotFile, json);
        } catch (IOException e) {
            ServerUtilities.LOGGER.error("Failed to write snapshot file {}", snapshotFile, e);
        }
    }

    public String getName() {
        return NAME_FORMAT.format(createdAt);
    }

    /** @return the most recent readable snapshot in the destination, or null if there is none */
    private static Snapshot findLatest(File destination) {
        File[] files = destination.listFiles(f -> f.isFile() && SNAPSHOT_FILE.matcher(f.getName()).matches());
        if (files == null || files.length == 0) return null;

        Arrays.sort(files, Comparator.comparing(File::getName).reversed());

        for (File file : files) {
            try {
                return new Snapshot(destination, SnapshotJson.read(file));
            } catch (Exception e) {
                // an interrupted run can leave a corrupt file, fall back to the one before it
                ServerUtilities.LOGGER.error("Failed to read snapshot file {}", file, e);
            }
        }
        return null;
    }

    private void writeDimensions(File manifests, PackWriter packWriter, ChunkReader reader, Snapshot previousSnapshot) throws IOException {
        int unmodifiedFiles = 0;
        int unmodifiedChunks = 0;

        for (SnapshotDimension dimension : dimensions.values()) {
            SnapshotDimension previousDimension = previousSnapshot == null ? null :previousSnapshot.dimensions.get(dimension.id);

            for (File regionFile : dimension.regionsFiles) {
                SnapshotDimension.Region previousRegion = previousDimension == null ? null : previousDimension.getRegion(regionFile.getName());
                if (previousRegion != null) {
                    if (previousRegion.mtime == regionFile.lastModified() && previousRegion.size == regionFile.length()) {
                        dimension.registerRegion(previousRegion);
                        unmodifiedFiles++;
                        continue;
                    }
                }

                RegionChunkFile regionChunkFile = new RegionChunkFile(regionFile.toPath());

                List<ChunkBlob> blobs = Collections.emptyList();
                try {
                    blobs = reader.readChunks(regionChunkFile);
                } catch (Exception e) {
                    ServerUtilities.LOGGER.error("Failed to read chunks from region file {}", regionFile, e);
                }

                for (int i = 0; i < blobs.size(); i++) {
                    ChunkBlob blob = blobs.get(i);
                    if (previousRegion == null) continue;
                    ChunkBlob.Metadata previousMeta = previousRegion.getChunk(blob.metadata().index());
                    if (previousMeta == null) continue;

                    if (previousMeta.timestamp() == blob.metadata().timestamp()) {
                        blobs.set(i, new ChunkBlob.Empty(previousMeta));
                        unmodifiedChunks++;
                    } else if (Arrays.equals(blob.metadata().hash(), previousMeta.hash())) {
                        blobs.set(i, new ChunkBlob.Empty(previousMeta));
                        unmodifiedChunks++;
                    }
                }

                SnapshotDimension.Region region = dimension.registerRegion(regionFile, blobs);
                for (ChunkBlob blob : blobs) {
                    packWriter.add(blob);
                }

                writeRegionManifest(manifests, region);
            }
        }

        ServerUtilities.LOGGER.info("Snapshot completed with {} unmodified files and {} unmodified chunks", unmodifiedFiles, unmodifiedChunks);
    }

    public SnapshotJson toJson() {
        SnapshotJson json = new SnapshotJson();
        json.createdAt = createdAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        json.completedAt = completedAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        json.minecraftVersion = minecraftVersion;
        json.modVersion = modVersion;
        json.files = files;

        for (SnapshotDimension dimension : dimensions.values()) {
            SnapshotJson.Dimension dimensionJson = new SnapshotJson.Dimension();
            dimensionJson.id = dimension.id;

            for (SnapshotDimension.Region region : dimension.getRegions()) {
                ManifestDescriptor descriptor = new ManifestDescriptor();
                descriptor.manifest = region.getHash();
                descriptor.size = region.size;
                descriptor.mtime = region.mtime;
                dimensionJson.kinds.regions.put(region.name, descriptor);
            }

            json.dimensions.add(dimensionJson);
        }

        return json;
    }

    private Map<Integer, SnapshotDimension> getDimensions(File directory) {
        Map<Integer, SnapshotDimension> discovered = new HashMap<>();

        File[] directories = directory.listFiles(File::isDirectory);
        if (directories == null) throw new SnapshotException("No regions or dimensions were found in world folder, likely the specified world folder is incorrect");

        for (File dimensionDirectory : directories) {
            String dirName = dimensionDirectory.getName();
            File regionDirectory = dimensionDirectory;
            if (!dirName.startsWith("DIM") && !dirName.equals("region")) continue;

            int id = 0;
            if (dirName.startsWith("DIM")) {
                id = Integer.parseInt(dirName.substring(3));
                regionDirectory = new File(dimensionDirectory, "region");
            }

            File[] regionFiles = regionDirectory.listFiles(f -> f.getName().endsWith(".mca"));
            SnapshotDimension dimension = new SnapshotDimension(id, regionFiles != null ? Arrays.asList(regionFiles) : Collections.emptyList());
            discovered.put(dimension.id, dimension);
        }

        return discovered;
    }

    private Map<Integer, SnapshotDimension> getDimensions(File root, SnapshotJson json) {
        Map<Integer, SnapshotDimension> dimensions = new HashMap<>(json.dimensions.size());

        for (SnapshotJson.Dimension dimensionJson : json.dimensions) {
            SnapshotDimension dimension = new SnapshotDimension(dimensionJson.id, new ArrayList<>());
            dimension.populateFromJson(dimensionJson, root);
            dimensions.put(dimension.id, dimension);
        }

        return dimensions;
    }

    private static void writeRegionManifest(File rootDirectory, SnapshotDimension.Region region) {
        FileOutputStream outputStream = null;
        try {
            byte[] regionManifestData = region.toBytes();
            String regionHash = region.getHash();
            File manifestFile = new File(rootDirectory, regionHash);

            outputStream = new FileOutputStream(manifestFile);
            outputStream.write(regionManifestData);
        } catch (IOException e) {
            ServerUtilities.LOGGER.error("Failed to save region manifest {}", rootDirectory, e);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
    }

    private static File ensureSubDirectory(File root, String sub) {
        File subDir = new File(root, sub);
        if (!subDir.exists() && !subDir.mkdirs()) {
            throw new SnapshotException("Failed to create subdirectory " + sub);
        }
        return subDir;
    }
}
