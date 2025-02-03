package serverutils.lib.util.compression;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;

import serverutils.lib.util.CommonUtils;

public interface ICompress extends AutoCloseable {

    boolean useLegacy = !CommonUtils.getClassExists("org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream");

    void createOutputStream(File file) throws IOException;

    void addFileToArchive(File file, String name) throws IOException;

    void extractArchive(File archive, boolean includeGlobal, boolean isOldBackup) throws IOException;

    boolean isOldBackup(File archive) throws IOException;

    @Nullable
    String getWorldName(File file) throws IOException;

    static ICompress createCompressor() {
        return useLegacy ? new LegacyCompressor() : new CommonsCompressor();
    }
}
