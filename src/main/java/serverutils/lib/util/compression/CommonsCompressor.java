package serverutils.lib.util.compression;

import static serverutils.ServerUtilitiesConfig.backups;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;

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
        output = zaos;
    }

    @Override
    public void addFileToArchive(File file, String name) throws IOException {
        ArchiveEntry entry = output.createArchiveEntry(file, name);
        output.putArchiveEntry(entry);
        try (FileInputStream fis = new FileInputStream(file)) {
            IOUtils.copy(fis, output);
        }
        output.closeArchiveEntry();
    }

    @Override
    public void close() throws Exception {
        if (output != null) {
            output.close();
        }
    }
}
