package serverutils.task.backup;

import java.io.File;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import serverutils.lib.util.backup.SnapshotEntry;

import serverutils.lib.util.FileUtils;

public final class WorldFile {

    private static final Pattern REGION_PATH = Pattern.compile("region/r\\.-?\\d+\\.-?\\d+\\.mca");
    private final String relativePath;
    private final File backing;
    private final Kind kind;
    @Nullable
    public final String dimension;

    public WorldFile(@Nullable String dimension, String relativePath, File backing, Kind kind) {
        this.dimension = dimension;
        this.relativePath = relativePath;
        this.backing = backing;
        this.kind = kind;
    }

    public WorldFile(String relativePath, File backing, Kind kind) {
        this(null, relativePath, backing, kind);
    }

    /**
     * Classifies a file by its path relative to its dimension root. Only {@code region/r.<x>.<z>.mca} directly under
     * the dimension root is region data; anything else (including stray or nested .mca files) is misc.
     */
    public static WorldFile from(String dimension, File dimensionRoot, File location) {
        return from(dimension, dimensionRoot, dimensionRoot, location);
    }

    public static WorldFile from(String dimension, File dimensionRoot, File nameRoot, File location) {
        Kind kind = REGION_PATH.matcher(relativize(dimensionRoot, location)).matches() ? Kind.REGION : Kind.MISC;
        return new WorldFile(dimension, relativize(nameRoot, location), location, kind);
    }

    public static WorldFile from(String dimension, File root, String path) {
        return from(dimension, root, root, new File(root, path));
    }

    private static String relativize(File root, File location) {
        return root.toPath().relativize(location.toPath()).toString().replace('\\', '/');
    }

    public String name() {
        return backing.getName();
    }

    public boolean isRegionFile() {
        return kind == Kind.REGION;
    }

    public long mtime() {
        return backing.lastModified();
    }

    public long size() {
        return backing.length();
    }

    public String relativePath() {
        return relativePath;
    }

    public File backing() {
        return backing;
    }

    public Kind kind() {
        return kind;
    }

    public boolean isUnmodified(SnapshotEntry entry) {
        if (entry == null) return false;
        if (entry.kind() != kind) return false;
        return entry.mtime() == mtime() && entry.size() == size();
    }

    public String runtimeRelativePath() {
        return FileUtils.getRelativePath(backing);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (WorldFile) obj;
        return Objects.equals(this.relativePath, that.relativePath) && Objects.equals(this.backing, that.backing)
                && Objects.equals(this.kind, that.kind);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relativePath, backing, kind);
    }

    @Override
    public String toString() {
        return "WorldFile[" + "relativePath="
                + relativePath
                + ", "
                + "backing="
                + backing
                + ", "
                + "kind="
                + kind
                + ']';
    }

    public enum Kind {
        REGION,
        MISC
    }
}
