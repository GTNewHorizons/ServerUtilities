package latmod.lib.net;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.gson.JsonElement;

import latmod.lib.LMJsonUtils;
import latmod.lib.LMStringUtils;

public final class Response {

    public final long millis;
    public final int code;
    public final InputStream stream;

    public Response(long ms, int c, InputStream s) {
        millis = ms;
        code = c;
        stream = s;
    }

    public String toString() {
        return Integer.toString(code);
    }

    public String asString() throws Exception {
        return LMStringUtils.readString(stream);
    }

    public JsonElement asJson() throws Exception {
        return LMJsonUtils.fromJson(new BufferedReader(new InputStreamReader(stream)));
    }
}
