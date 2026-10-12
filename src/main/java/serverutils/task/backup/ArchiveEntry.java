package serverutils.task.backup;

import java.io.File;

import javax.annotation.Nullable;

public final class ArchiveEntry implements BackupEntry {

    public final File file;
    private final String world;
    private final long created;
    private final long size;
    private final boolean custom;
    private final String problem;

    public ArchiveEntry(File file, @Nullable String world, long created, long size, boolean custom,
            @Nullable String problem) {
        this.file = file;
        this.world = world;
        this.created = created;
        this.size = size;
        this.custom = custom;
        this.problem = problem;
    }

    @Override
    public String id() {
        return file.getName();
    }

    @Override
    public @Nullable String world() {
        return world;
    }

    @Override
    public long createdAt() {
        return created;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public boolean isCustom() {
        return custom;
    }

    @Override
    public @Nullable String problem() {
        return problem;
    }
}