package serverutils.lib.util.compression;

import static serverutils.ServerUtilitiesConfig.backups;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;

import javax.annotation.Nullable;

import net.minecraftforge.common.DimensionManager;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

public class CommonsCompressor implements ICompress {

    private ArchiveOutputStream output;

    @Override
    public void createOutputStream(File file) throws IOException {
        ZipArchiveOutputStream zaos = new ZipArchiveOutputStream(file);
        if (backups.compression_level == 0) {
            zaos.setMethod(ZipEntry.STORED);
        } else {
            zaos.setLevel(backups.compression_level);
        }

        File worldDir = DimensionManager.getCurrentSaveRootDirectory();
        if (worldDir != null) {
            zaos.setComment(worldDir.getName());
        }

        output = zaos;
    }

    @Override
    public void addFileToArchive(File file, String name) throws IOException {
        ArchiveEntry entry = output.createArchiveEntry(file, name);
        output.putArchiveEntry(entry);
        try (FileInputStream fis = new FileInputStream(file)) {
            ICompress.copyInterruptibly(fis, output);
        }
        output.closeArchiveEntry();
    }

    @Override
    public boolean isOldBackup(File archive) throws IOException {
        return ArchiveExtraction.isOldBackup(archive);
    }

    @Override
    public void extractArchive(File archive, boolean includeGlobal, boolean isOldBackup) throws IOException {

        ArchiveExtraction.extract(archive, includeGlobal, isOldBackup);
    }

    @Override
    public @Nullable String getWorldName(File file) throws IOException {
        if (file.isDirectory() || !file.getName().endsWith(".zip")) return null;
        // uses native zip file implementation because reading the
        // comment from a commons compress ZipFile is significantly slower
        try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(file)) {
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
