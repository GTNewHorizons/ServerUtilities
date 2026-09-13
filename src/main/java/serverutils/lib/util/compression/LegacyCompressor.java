package serverutils.lib.util.compression;

import static serverutils.ServerUtilitiesConfig.backups;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nullable;

import net.minecraftforge.common.DimensionManager;

public class LegacyCompressor implements ICompress {

    private ZipOutputStream output;

    @Override
    public void createOutputStream(File file) throws IOException {
        output = new ZipOutputStream(new FileOutputStream(file));
        if (backups.compression_level == 0) {
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
        output.putNextEntry(entry);
        try (FileInputStream fis = new FileInputStream(file)) {
            ICompress.copyInterruptibly(fis, output);
        }
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
