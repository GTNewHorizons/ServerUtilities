package serverutils.lib.util.compression;

import java.io.File;
import java.io.IOException;

public interface ICompress extends AutoCloseable {

    void createOutputStream(File file) throws IOException;

    void addFileToArchive(File file, String name) throws IOException;
}
