package serverutils.task.backup.repository;

import java.io.File;
import java.util.Properties;

import javax.annotation.Nullable;

/** The header of one archive. Its file list is stored as chunks and read with {@link Repository#readItems}. */
public final class ArchiveInfo {

    public final String name;
    public final File file;
    final Properties header;
    final byte[] metadataChunks;

    ArchiveInfo(String name, File file, Properties header, byte[] metadataChunks) {
        this.name = name;
        this.file = file;
        this.header = header;
        this.metadataChunks = metadataChunks;
    }

    /** Creation time, in milliseconds since the epoch. */
    public long created() {
        return Long.parseLong(header.getProperty("createdAt"));
    }

    public boolean isCustomName() {
        return Boolean.parseBoolean(header.getProperty("customName"));
    }

    /** The persistent world UUID, if the world had one when the backup was made. */
    @Nullable
    public String worldId() {
        return header.getProperty("worldId");
    }

    /** The world folder name, like a ZIP backup's comment. */
    @Nullable
    public String worldName() {
        return header.getProperty("worldName");
    }

    /** The world folder relative to the instance, with a trailing slash, such as {@code World/} or {@code saves/x/}. */
    @Nullable
    public String worldPath() {
        return header.getProperty("worldPath");
    }

    public int fileCount() {
        return Integer.parseInt(header.getProperty("files", "0"));
    }

    /** Total size of the files in this archive, before deduplication and compression. */
    public long originalSize() {
        return Long.parseLong(header.getProperty("size", "0"));
    }
}
