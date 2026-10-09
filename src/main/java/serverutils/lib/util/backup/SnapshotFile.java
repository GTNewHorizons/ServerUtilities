package serverutils.lib.util.backup;

import com.github.bsideup.jabel.Desugar;

/**
 * A generic file containing data for a dimension
 */
@Desugar
public record SnapshotFile(String path, long mtime, long size, SHAHash hash) implements Hashable {

    public SnapshotFile(String path, SHAHash hash, long size, long mtime) {
        this(path, mtime, size, hash);
    }
}
