package serverutils.task.backup;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public interface BackupStore<T extends BackupEntry> {

    List<T> list() throws IOException;

    void delete(Collection<T> entries, BackupResult.Retention result) throws IOException;

    /**
     * The bytes these entries take up on top of what {@code base} already uses, which is what size limits are compared
     * against. The two collections never overlap. Independent entries just add up their own sizes.
     */
    default long countedSize(Collection<T> entries, Collection<T> base) {
        return sumSizes(entries);
    }

    /** Called once a prune is done deleting, so stores with shared data can free what is no longer used. */
    default void reclaim() throws IOException {}

    static long sumSizes(Collection<? extends BackupEntry> entries) {
        long total = 0;
        for (BackupEntry entry : entries) total = Math.addExact(total, entry.size());
        return total;
    }
}
