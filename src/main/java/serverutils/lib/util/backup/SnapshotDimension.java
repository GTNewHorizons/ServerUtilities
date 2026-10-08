package serverutils.lib.util.backup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The regions of one dimension as recorded by a snapshot. */
public class SnapshotDimension {

    public final int id;

    private final Map<String, SnapshotRegion> regions = new HashMap<>();

    public SnapshotDimension(int id) {
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
}
