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

    private final Map<String, String> files;
    private final Map<Integer, SnapshotDimension> dimensions;

    public Snapshot(ZonedDateTime createdAt, ZonedDateTime completedAt, String minecraftVersion, String modVersion,
            Map<String, String> files, Map<Integer, SnapshotDimension> dimensions) {
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.minecraftVersion = minecraftVersion;
        this.modVersion = modVersion;
        this.files = files;
        this.dimensions = dimensions;
    }

    public String getName() {
        return NAME_FORMAT.format(createdAt);
    }

    public SnapshotDimension getDimension(int id) {
        return dimensions.get(id);
    }

    public Map<Integer, SnapshotDimension> getDimensions() {
        return Collections.unmodifiableMap(dimensions);
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

            for (SnapshotRegion region : dimension.getRegions()) {
                ManifestDescriptor descriptor = new ManifestDescriptor();
                descriptor.manifest = region.getHash().toString();
                descriptor.size = region.size;
                descriptor.mtime = region.mtime;
                dimensionJson.kinds.regions.put(region.name, descriptor);
            }

            json.dimensions.add(dimensionJson);
        }

        return json;
    }

    /** @param manifests where region manifests are stored, keyed by their hash */
    public static Snapshot fromJson(ManifestStore manifests, SnapshotJson json) {
        return fromJson(manifests, json, false);
    }

    public static Snapshot fromJson(ManifestStore manifests, SnapshotJson json, boolean strict) {
        Map<Integer, SnapshotDimension> dimensions = new HashMap<>(json.dimensions.size());

        for (SnapshotJson.Dimension dimensionJson : json.dimensions) {
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

            dimensions.put(dimension.id, dimension);
        }

        return new Snapshot(
                ZonedDateTime.parse(json.createdAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                ZonedDateTime.parse(json.completedAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                json.minecraftVersion,
                json.modVersion,
                json.files,
                dimensions);
    }
}
