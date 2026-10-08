package serverutils.lib.util.compression;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

public class CustomZipArchiveOutputStream extends ZipArchiveOutputStream {

    public CustomZipArchiveOutputStream(OutputStream out) {
        super(out);
    }

    public CustomZipArchiveOutputStream(File file) throws IOException {
        super(file);
    }

    public static ZipArchiveOutputStream buffered(File file, long size) throws FileNotFoundException {
        return new CustomZipArchiveOutputStream(new BufferedOutputStream(new FileOutputStream(file), (int) size));
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } catch (IOException | RuntimeException | Error failure) {
            // Commons Compress 1.8 skips closing the file when finish() rejects an incomplete entry.
            finished = true;
            try {
                super.close();
            } catch (IOException cleanup) {
                failure.addSuppressed(cleanup);
            }
            throw failure;
        } finally {
            def.end();
        }
    }
}
