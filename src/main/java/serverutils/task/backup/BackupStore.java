package serverutils.task.backup;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public interface BackupStore<T extends BackupEntry> {

    List<T> list() throws IOException;

    void delete(Collection<T> entries, BackupResult.Retention result) throws IOException;

    /**
     * The bytes these entries occupy together, which is what size limits are compared against.
     */
    default long countedSize(Collection<T> entries) {
        return sumSizes(entries);
    }

    static long sumSizes(Collection<? extends BackupEntry> entries) {
        long total = 0;
        for (BackupEntry entry : entries) total = Math.addExact(total, entry.size());
        return total;
    }
}
