package serverutils.lib.util.compression;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.concurrent.Callable;

import javax.annotation.Nullable;

import serverutils.lib.util.CommonUtils;

public interface ICompress extends AutoCloseable {

    boolean useLegacy = !CommonUtils.getClassExists("org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream");

    void createOutputStream(File file) throws IOException;

    void addFileToArchive(File file, String name) throws IOException;

    static void copyInterruptibly(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[8192];
        while (true) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Backup cancelled");
            int length = input.read(buffer);
            if (length == -1) return;
            output.write(buffer, 0, length);
        }
    }

    void extractArchive(File archive, boolean includeGlobal, boolean isOldBackup, File preserved, File recovery)
            throws IOException;

    default void extractArchive(File archive, boolean includeGlobal, boolean isOldBackup, File preserved, File recovery,
            Callable<Void> beforeInstall) throws IOException {
        ArchiveExtraction.extract(archive, includeGlobal, isOldBackup, preserved, recovery, beforeInstall);
    }

    boolean isOldBackup(File archive) throws IOException;

    static void validateRestoreTargets(File archive, String worldName, boolean legacy) throws IOException {
        ArchiveExtraction.validateRestoreTargets(archive, worldName, legacy);
    }

    static void validateRestoreTargets(File archive, String worldName, boolean legacy, boolean includeGlobal)
            throws IOException {
        ArchiveExtraction.validateRestoreTargets(archive, worldName, legacy, includeGlobal);
    }

    @Nullable
    String getWorldName(File file) throws IOException;

    static ICompress createCompressor() {
        return useLegacy ? new LegacyCompressor() : new CommonsCompressor();
    }
}
