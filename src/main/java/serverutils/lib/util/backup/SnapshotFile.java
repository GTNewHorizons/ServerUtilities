package serverutils.lib.util.backup;

import com.github.bsideup.jabel.Desugar;

import serverutils.task.backup.WorldFile;

/**
 * A generic file containing data for a dimension
 */
@Desugar
public record SnapshotFile(String path, long mtime, long size, SHAHash hash) implements SnapshotEntry {

    public SnapshotFile(String path, SHAHash hash, long size, long mtime) {
        this(path, mtime, size, hash);
    }

    @Override
    public WorldFile.Kind kind() {
        return WorldFile.Kind.MISC;
    }
}
