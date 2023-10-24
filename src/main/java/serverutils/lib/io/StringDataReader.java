package serverutils.lib.io;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import serverutils.lib.util.JsonUtils;
import serverutils.lib.util.StringUtils;

public class StringDataReader extends DataReader {

    public final String string;

    StringDataReader(@Nullable String s) {
        string = StringUtils.emptyIfNull(s);
    }

    public String toString() {
        return string;
    }

    @Override
    public boolean canRead() {
        return !string.isEmpty();
    }

    @Override
    public String string() {
        return string;
    }

    @Override
    public String string(int bufferSize) {
        return string;
    }

    @Override
    public List<String> stringList() throws Exception {
        BufferedReader reader = new BufferedReader(new StringReader(string));
        List<String> l = new ArrayList<>();
        String s;
        while ((s = reader.readLine()) != null) {
            l.add(s);
        }
        return l;
    }

    @Override
    public JsonElement json() throws Exception {
        if (string.equals("null")) {
            return JsonNull.INSTANCE;
        } else if (string.equals("true")) {
            return JsonUtils.JSON_TRUE;
        } else if (string.equals("false")) {
            return JsonUtils.JSON_FALSE;
        } else if (string.length() == 2) {
            char c0 = string.charAt(0);
            char c1 = string.charAt(1);

            if (c0 == '[' && c1 == ']') {
                return new JsonArray();
            } else if (c0 == '{' && c1 == '}') {
                return new JsonObject();
            } else if (c0 == '"' && c1 == '"') {
                return JsonUtils.JSON_EMPTY_STRING;
            }
        }

        try (Reader reader = new StringReader(string)) {
            return JsonUtils.parse(reader);
        }
    }

    @Override
    public BufferedImage image() {
        throw new IllegalStateException("Can't parse string as image!");
    }
}
