package serverutils.lib.util.backup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/** Compression of file blobs, the type ids match the ones used by the region format. */
public final class BlobCompression {

    public static final int NONE = 0;
    public static final int GZIP = 1;
    public static final int ZLIB = 2;

    private BlobCompression() {}

    /** @return the zlib compressed data */
    public static byte[] deflate(byte[] data) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(Math.max(32, data.length / 2));
        try (DeflaterOutputStream out = new DeflaterOutputStream(bytes)) {
            out.write(data);
        }
        return bytes.toByteArray();
    }

    public static byte[] decompress(int type, byte[] data) throws IOException {
        switch (type) {
            case NONE:
                return data;
            case GZIP:
                return readAll(new GZIPInputStream(new ByteArrayInputStream(data)));
            case ZLIB:
                return readAll(new InflaterInputStream(new ByteArrayInputStream(data)));
            default:
                throw new IOException("Unknown compression type " + type);
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        try (InputStream stream = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }
}
