package serverutils.lib.util.compression;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Random;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CompressorCancellationTest {

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void bothCompressorsObserveCancellationDuringFileCopy() throws Exception {
        byte[] data = new byte[256 * 1024];
        new Random(0).nextBytes(data);
        File source = temporary.newFile();
        Files.write(source.toPath(), data);
        for (boolean legacy : new boolean[] { false, true }) {
            for (boolean cancel : new boolean[] { false, true }) {
                ByteArrayOutputStream sink = new ByteArrayOutputStream() {

                    private boolean interrupted;

                    @Override
                    public synchronized void write(byte[] bytes, int offset, int length) {
                        super.write(bytes, offset, length);
                        // Interrupt only after compressed payload has been written, not during the ZIP header.
                        if (cancel && !interrupted && size() > 32768) {
                            interrupted = true;
                            Thread.currentThread().interrupt();
                        }
                    }
                };
                ICompress compressor = legacy ? new LegacyCompressor() : new CommonsCompressor();
                OutputStream output = legacy ? new ZipOutputStream(sink) : new ZipArchiveOutputStream(sink);
                Field field = compressor.getClass().getDeclaredField("output");
                field.setAccessible(true);
                field.set(compressor, output);
                try {
                    if (cancel) {
                        assertThrows(InterruptedIOException.class, () -> compressor.addFileToArchive(source, "data"));
                        assertTrue(Thread.currentThread().isInterrupted());
                        assertTrue("Copy must stop before reading the complete file", sink.size() < data.length);
                    } else {
                        compressor.addFileToArchive(source, "data");
                    }
                } finally {
                    Thread.interrupted();
                    try {
                        // Finish the deliberately interrupted entry to release the test stream cleanly.
                        if (cancel) {
                            if (legacy) ((ZipOutputStream) output).closeEntry();
                            else((ZipArchiveOutputStream) output).closeArchiveEntry();
                        }
                    } finally {
                        compressor.close();
                    }
                }
                if (!cancel) {
                    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(sink.toByteArray()))) {
                        assertEquals("data", zip.getNextEntry().getName());
                        assertArrayEquals(data, IOUtils.toByteArray(zip));
                    }
                }
            }
        }
    }
}
