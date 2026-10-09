package serverutils.lib.util.backup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The regions and other files of one dimension as recorded by a snapshot. */
public class SnapshotDimension {

    /** The id of the dimension stored in the world folder itself, every other id is the name of its folder. */
    public static final String OVERWORLD = "overworld";

    public final String id;

    private final Map<String, SnapshotRegion> regions = new HashMap<>();
    private final Map<String, SnapshotFile> files = new HashMap<>();

    public SnapshotDimension(String id) {
        this.id = id;
    }

    public List<SnapshotRegion> getRegions() {
        return new ArrayList<>(regions.values());
    }

    public SnapshotRegion getRegion(String name) {
        return regions.get(name);
    }

    public void addRegion(SnapshotRegion region) {
        regions.put(region.name, region);
    }

    public List<SnapshotFile> getFiles() {
        return new ArrayList<>(files.values());
    }

    public SnapshotFile getFile(String path) {
        return files.get(path);
    }

    public void addFile(SnapshotFile file) {
        files.put(file.path(), file);
    }

    public String getDirName() {
        return id;
    }

    public boolean isOverworld() {
        return id.equals(OVERWORLD);
    }
}
