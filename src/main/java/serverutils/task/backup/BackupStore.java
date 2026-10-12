package serverutils.task.backup;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public interface BackupStore<T extends BackupEntry> {

    List<T> list() throws IOException;

    void delete(Collection<T> entries, BackupResult.Retention result) throws IOException;
}