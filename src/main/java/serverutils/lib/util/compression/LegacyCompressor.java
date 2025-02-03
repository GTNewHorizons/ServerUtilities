package serverutils.lib.util.compression;

import static serverutils.ServerUtilitiesConfig.backups;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nullable;

import net.minecraftforge.common.DimensionManager;

import org.apache.commons.io.IOUtils;

import serverutils.lib.util.FileUtils;

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
            IOUtils.copy(fis, output);
        }
        output.closeEntry();
    }

    @Override
    public void extractArchive(File archive, boolean includeGlobal) throws IOException {
        try (ZipFile zip = new ZipFile(archive)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                InputStream in = zip.getInputStream(entry);
                File file = FileUtils.newFile(new File(entry.getName()));
                OutputStream out = new FileOutputStream(file);
                IOUtils.copy(in, out);

                in.close();
                out.close();
            }
        }
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
