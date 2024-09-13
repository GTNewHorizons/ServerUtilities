package serverutils.lib.util.compression;

import static serverutils.ServerUtilitiesConfig.backups;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

public class LegacyCompressor implements ICompress {

    private ZipOutputStream output;

    @Override
    public void createOutputStream(File file) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file));
        if (backups.compression_level == 0) {
            zos.setMethod(ZipOutputStream.STORED);
        } else {
            zos.setLevel(backups.compression_level);
        }

        output = zos;
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
    public void close() throws Exception {
        if (output != null) {
            output.close();
        }
    }
}
