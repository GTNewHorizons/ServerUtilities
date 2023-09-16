package latmod.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;

/**
 * Type for Lists: new TypeToken<List<E>>() {}.getType()
 */
public class LMJsonUtils {

    private static Gson gson = null;
    private static Gson gson_pretty = null;
    public static JsonDeserializationContext deserializationContext;
    public static JsonSerializationContext serializationContext, prettySerializationContext;

    public static Gson getGson(boolean pretty) {
        if (gson == null || gson_pretty == null) {
            GsonBuilder gb = new GsonBuilder();
            gson = gb.create();
            gb.setPrettyPrinting();
            gson_pretty = gb.create();

            deserializationContext = new JsonDeserializationContext() {

                @SuppressWarnings("unchecked")
                public <T> T deserialize(JsonElement json, Type typeOfT) throws JsonParseException {
                    return (T) gson.fromJson(json, typeOfT);
                }
            };

            serializationContext = new JsonSerializationContext() {

                public JsonElement serialize(Object src) {
                    return gson.toJsonTree(src);
                }

                public JsonElement serialize(Object src, Type typeOfSrc) {
                    return gson.toJsonTree(src, typeOfSrc);
                }
            };

            prettySerializationContext = new JsonSerializationContext() {

                public JsonElement serialize(Object src) {
                    return gson_pretty.toJsonTree(src);
                }

                public JsonElement serialize(Object src, Type typeOfSrc) {
                    return gson_pretty.toJsonTree(src, typeOfSrc);
                }
            };
        }

        return pretty ? gson_pretty : gson;
    }

    public static String toJson(Gson gson, JsonElement e) {
        if (e == null) return null;
        return gson.toJson(e);
    }

    public static boolean toJson(Gson gson, File f, JsonElement o) {
        if (o == null) return false;

        try {
            String s = toJson(gson, o);
            LMFileUtils.save(f, s);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static String toJson(JsonElement o) {
        return toJson(getGson(false), o);
    }

    public static boolean toJson(File f, JsonElement o) {
        return toJson(getGson(true), f, o);
    }

    public static JsonElement fromJson(String json) {
        return (json == null || json.isEmpty()) ? JsonNull.INSTANCE : new JsonParser().parse(json);
    }

    public static JsonElement fromJson(Reader json) {
        return (json == null) ? JsonNull.INSTANCE : new JsonParser().parse(json);
    }

    public static JsonElement fromJson(File json) {
        try {
            if (json == null || !json.exists()) return JsonNull.INSTANCE;
            BufferedReader reader = new BufferedReader(new FileReader(json));
            JsonElement e = fromJson(reader);
            reader.close();
            return e;
        } catch (Exception ex) {}
        return JsonNull.INSTANCE;
    }

    public static JsonArray join(JsonArray... a) {
        JsonArray a1 = new JsonArray();

        for (JsonArray anA : a) {
            if (anA != null) {
                for (int j = 0; j < anA.size(); j++) a1.add(anA.get(j));
            }
        }

        return a1;
    }

    // -- //

    public static JsonArray toIntArray(int[] ai) {
        if (ai == null) return null;
        JsonArray a = new JsonArray();
        if (ai.length == 0) return a;
        for (int anAi : ai) a.add(new JsonPrimitive(anAi));
        return a;
    }

    public static int[] fromIntArray(JsonElement e) {
        if (e == null || e.isJsonNull()) return null;

        if (e.isJsonArray()) {
            JsonArray a = e.getAsJsonArray();
            int[] ai = new int[a.size()];
            if (ai.length == 0) return ai;
            for (int i = 0; i < ai.length; i++) ai[i] = a.get(i).getAsInt();
            return ai;
        }

        return new int[] { e.getAsInt() };
    }

    public static JsonArray toNumberArray(Number[] ai) {
        if (ai == null) return null;
        JsonArray a = new JsonArray();
        if (ai.length == 0) return a;
        for (Number anAi : ai) a.add(new JsonPrimitive(anAi));
        return a;
    }

    public static Number[] fromNumberArray(JsonElement e) {
        if (e == null || e.isJsonNull()) return null;

        if (e.isJsonArray()) {
            JsonArray a = e.getAsJsonArray();
            Number[] ai = new Number[a.size()];
            if (ai.length == 0) return ai;
            for (int i = 0; i < ai.length; i++) ai[i] = a.get(i).getAsNumber();
            return ai;
        }

        return new Number[] { e.getAsNumber() };
    }

    public static JsonArray toStringArray(String[] ai) {
        if (ai == null) return null;
        JsonArray a = new JsonArray();
        if (ai.length == 0) return a;
        for (String anAi : ai) a.add(new JsonPrimitive(anAi));
        return a;
    }

    public static String[] fromStringArray(JsonElement e) {
        if (e == null || e.isJsonNull() || !e.isJsonArray()) return null;
        JsonArray a = e.getAsJsonArray();
        String[] ai = new String[a.size()];
        if (ai.length == 0) return ai;
        for (int i = 0; i < ai.length; i++) ai[i] = a.get(i).getAsString();
        return ai;
    }

    public static JsonObject join(JsonObject... o) {
        JsonObject o1 = new JsonObject();

        for (JsonObject anO : o) {
            if (anO != null) {
                for (Map.Entry<String, JsonElement> e : anO.entrySet()) o1.add(e.getKey(), e.getValue());
            }
        }

        return o1;
    }

    public static void printPretty(JsonElement e) {
        System.out.println(toJson(getGson(true), e));
    }

    public static List<JsonElement> deserializeText(List<String> text) {
        List<JsonElement> elements = new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        int inc = 0;

        for (String s : text) {
            s = LMStringUtils.trimAllWhitespace(s);

            System.out.println(s);

            if (s.isEmpty()) {
                elements.add(JsonNull.INSTANCE);
            } else {
                if (inc > 0 || s.startsWith("{")) {
                    for (int i = 0; i < s.length(); i++) {
                        char c = s.charAt(i);
                        if (c == '{') inc++;
                        else if (c == '}') inc--;
                        sb.append(c);

                        if (inc == 0) {
                            System.out.println(":: " + sb.toString());
                            elements.add(fromJson(sb.toString()));
                            sb.setLength(0);
                        }
                    }
                } else {
                    elements.add(new JsonPrimitive(s));
                }
            }
        }

        return elements;
    }
}
