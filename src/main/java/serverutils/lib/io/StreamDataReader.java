package serverutils.lib.io;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.imageio.ImageIO;

import com.google.gson.JsonElement;

import serverutils.lib.util.JsonUtils;

public class StreamDataReader extends DataReader {

    public final InputStream stream;

    StreamDataReader(InputStream s) {
        stream = s;
    }

    public String toString() {
        return stream.toString();
    }

    @Override
    public String string(int bufferSize) throws Exception {
        try {
            return readStringFromStream(stream, bufferSize);
        } finally {
            stream.close();
        }
    }

    @Override
    public List<String> stringList() throws Exception {
        try {
            return readStringListFromStream(stream);
        } finally {
            stream.close();
        }
    }

    @Override
    public JsonElement json() throws Exception {
        try {
            return JsonUtils.parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } finally {
            stream.close();
        }
    }

    @Override
    public BufferedImage image() throws Exception {
        try {
            return ImageIO.read(stream);
        } finally {
            stream.close();
        }
    }
}
