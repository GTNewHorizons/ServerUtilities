package serverutils.task.backup;

import java.io.File;

import javax.annotation.Nullable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@AllArgsConstructor
@Accessors(fluent = true)
public final class ArchiveEntry implements BackupEntry {

    public final File file;
    @Nullable
    private final String world;
    private final long createdAt;
    private final long size;
    private final boolean isCustom;
    @Nullable
    private final String problem;

    @Override
    public String id() {
        return file.getName();
    }
}
