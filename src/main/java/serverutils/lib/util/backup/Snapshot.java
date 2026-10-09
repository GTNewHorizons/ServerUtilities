package serverutils.lib.util.backup;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import serverutils.ServerUtilities;

public class Snapshot {

    private static final DateTimeFormatter NAME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss'Z'");
    public static final Pattern FILE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}-\\d{2}-\\d{2}Z\\.json");

    public final ZonedDateTime createdAt;
    public final ZonedDateTime completedAt;
    public final String minecraftVersion;
    public final String modVersion;

    private final Map<String, SnapshotDimension> dimensions;

    public Snapshot(ZonedDateTime createdAt, ZonedDateTime completedAt, String minecraftVersion, String modVersion,
            Map<String, SnapshotDimension> dimensions) {
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.minecraftVersion = minecraftVersion;
        this.modVersion = modVersion;
        this.dimensions = dimensions;
    }

    public String getName() {
        return NAME_FORMAT.format(createdAt);
    }

    public SnapshotDimension getDimension(String id) {
        return dimensions.get(id);
    }

    public Map<String, SnapshotDimension> getDimensions() {
        return Collections.unmodifiableMap(dimensions);
    }

    public SnapshotManifest toJson() {
        SnapshotManifest json = new SnapshotManifest();
        json.createdAt = createdAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        json.completedAt = completedAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        json.minecraftVersion = minecraftVersion;
        json.modVersion = modVersion;

        for (SnapshotDimension dimension : dimensions.values()) {
            SnapshotManifest.Dimension dimensionJson = new SnapshotManifest.Dimension();
            dimensionJson.id = dimension.id;

            for (SnapshotRegion region : dimension.getRegions()) {
                dimensionJson.kinds.regions.put(region.name, descriptor(region.hash(), region.size, region.mtime));
            }

            for (SnapshotFile file : dimension.getFiles()) {
                dimensionJson.kinds.files.put(file.path(), descriptor(file.hash(), file.size(), file.mtime()));
            }

            json.dimensions.add(dimensionJson);
        }

        return json;
    }

    private static ManifestDescriptor descriptor(SHAHash hash, long size, long mtime) {
        ManifestDescriptor descriptor = new ManifestDescriptor();
        descriptor.manifest = hash.toString();
        descriptor.size = size;
        descriptor.mtime = mtime;
        return descriptor;
    }

    /** @param store where the region manifests and file blobs are stored, keyed by their hash */
    public static Snapshot fromJson(SnapshotStore store, SnapshotManifest json) {
        return fromJson(store, json, false);
    }

    public static Snapshot fromJson(SnapshotStore store, SnapshotManifest json, boolean strict) {
        PackStore manifests = store.manifests();
        PackStore files = store.files();
        Map<String, SnapshotDimension> dimensions = new HashMap<>(json.dimensions.size());

        for (SnapshotManifest.Dimension dimensionJson : json.dimensions) {
            SnapshotDimension dimension = new SnapshotDimension(dimensionJson.id);

            for (Map.Entry<String, ManifestDescriptor> entry : dimensionJson.kinds.regions.entrySet()) {
                ManifestDescriptor descriptor = entry.getValue();
                SHAHash hash = new SHAHash(descriptor.manifest);
                if (!manifests.contains(hash)) {
                    if (strict) {
                        throw new SnapshotException(
                                "Region manifest " + descriptor.manifest + " for " + entry.getKey() + " is missing");
                    }
                    ServerUtilities.LOGGER.warn(
                            "Region manifest {} for {} is missing, it will not be reused",
                            descriptor.manifest,
                            entry.getKey());
                    continue;
                }
                dimension.addRegion(SnapshotRegion.stored(entry.getKey(), hash, descriptor, manifests));
            }

            for (Map.Entry<String, ManifestDescriptor> entry : dimensionJson.kinds.files.entrySet()) {
                ManifestDescriptor descriptor = entry.getValue();
                SHAHash hash = new SHAHash(descriptor.manifest);
                if (!files.contains(hash)) {
                    if (strict) {
                        throw new SnapshotException(
                                "File " + descriptor.manifest + " for " + entry.getKey() + " is missing");
                    }
                    ServerUtilities.LOGGER.warn(
                            "File {} for {} is missing, it will not be reused",
                            descriptor.manifest,
                            entry.getKey());
                    continue;
                }
                dimension.addFile(new SnapshotFile(entry.getKey(), hash, descriptor.size, descriptor.mtime));
            }

            dimensions.put(dimension.id, dimension);
        }

        return new Snapshot(
                json.getCreatedAt(),
                json.getCompletedAt(),
                json.minecraftVersion,
                json.modVersion,
                dimensions);
    }
}
