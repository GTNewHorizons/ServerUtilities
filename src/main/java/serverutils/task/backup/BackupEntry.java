package serverutils.task.backup;

import javax.annotation.Nullable;

/** One restorable backup, whatever its storage format. */
public interface BackupEntry {

    /** Display name, unique within its store. */
    String id();

    /** The world this backup belongs to, or null if it could not be read. */
    @Nullable
    String world();

    /** Epoch millis. */
    long createdAt();

    /** Bytes freed by deleting only this entry, or 0 when the store does not track size. */
    long size();

    boolean isCustom();

    /** Why this entry could not be read; such entries are always preserved. */
    @Nullable
    String problem();
}