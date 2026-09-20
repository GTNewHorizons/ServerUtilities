package serverutils.lib.util.compression;

import static serverutils.ServerUtilitiesConfig.backups;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nullable;

import net.minecraftforge.common.DimensionManager;

public class LegacyCompressor implements ICompress {

    private ZipOutputStream output;
    private boolean stored;

    @Override
    public void createOutputStream(File file) throws IOException {
        output = new ZipOutputStream(new FileOutputStream(file));
        stored = backups.compression_level == 0;
        if (stored) {
            output.setMethod(ZipOutputStream.STORED);
        } else {
            output.setLevel(backups.compression_level);
        }

        File worldDir = DimensionManager.getCurrentSaveRootDirectory();
        if (worldDir != null) {
            output.setComment(worldDir.getName());
        }
    }

    @Override
    public void addFileToArchive(File file, String name) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        if (stored) {
            // java.util.zip requires size and CRC before opening a STORED entry.
            CRC32 checksum = new CRC32();
            long size = 0;
            byte[] buffer = new byte[8192];
            try (InputStream input = new FileInputStream(file)) {
                while (true) {
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Backup cancelled");
                    int length = input.read(buffer);
                    if (length == -1) break;
                    checksum.update(buffer, 0, length);
                    size += length;
                }
            }
            entry.setSize(size);
            entry.setCrc(checksum.getValue());
        }
        output.putNextEntry(entry);
        try (FileInputStream fis = new FileInputStream(file)) {
            ICompress.copyInterruptibly(fis, output);
        }
        output.closeEntry();
    }

    @Override
    public void addStreamToArchive(InputStream input, ZipEntry entry) throws IOException {
        output.putNextEntry(new ZipEntry(entry));
        ICompress.copyEntry(input, output, entry);
        output.closeEntry();
    }

    @Override
    public boolean isOldBackup(File archive) throws IOException {
        return ArchiveExtraction.isOldBackup(archive);
    }

    @Override
    public void extractArchive(File archive, boolean includeGlobal, boolean isOldBackup, File preserved, File recovery)
            throws IOException {
        ArchiveExtraction.extract(archive, includeGlobal, isOldBackup, preserved, recovery);
    }

    @Override
    public @Nullable String getWorldName(File file) throws IOException {
        if (file.isDirectory() || !file.getName().endsWith(".zip")) return null;
        try (ZipFile zipFile = new ZipFile(file)) {
            return zipFile.getComment();
        }
    }

    @Override
    public void close() throws Exception {
        if (output != null) {
            output.close();
        }
    }
}
