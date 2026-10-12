package serverutils.lib.util.backup;

import serverutils.task.backup.WorldFile;

public interface SnapshotEntry extends Hashable {
    long mtime();
    long size();
    WorldFile.Kind kind();
}
