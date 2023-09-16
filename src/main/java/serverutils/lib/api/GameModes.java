package serverutils.lib.api;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import latmod.lib.LMFileUtils;
import latmod.lib.LMJsonUtils;
import serverutils.lib.ServerUtilitiesLib;

public class GameModes {

    public final Map<String, GameMode> modes;
    public final GameMode defaultMode;
    public final GameMode commonMode;
    public final Map<String, String> customData;

    private static JsonObject createDefault() {
        JsonObject o = new JsonObject();
        JsonArray a = new JsonArray();
        a.add(new JsonPrimitive("default"));
        o.add("modes", a);
        o.add("default", new JsonPrimitive("default"));
        o.add("common", new JsonPrimitive("common"));
        return o;
    }

    private static boolean isValid(JsonElement e) {
        if (e == null || !e.isJsonObject()) return false;
        JsonObject o = e.getAsJsonObject();
        if (o == null || o.entrySet().isEmpty()) return false;
        return o.has("modes") && o.has("default") && o.has("common");
    }

    public GameModes(JsonElement el) {
        JsonObject o = isValid(el) ? el.getAsJsonObject() : createDefault();

        HashMap<String, GameMode> modes0 = new HashMap<>();

        JsonArray a = o.get("modes").getAsJsonArray();

        for (int i = 0; i < a.size(); i++) {
            GameMode m = new GameMode(a.get(i).getAsString());
            modes0.put(m.getID(), m);
        }

        defaultMode = modes0.get(o.get("default").getAsString());

        String common = o.get("common").getAsString();
        if (modes0.containsKey(common))
            throw new RuntimeException("ServerUtilitiesLibrary: common mode name can't be one of 'modes'!");
        commonMode = new GameMode(common);

        modes = Collections.unmodifiableMap(modes0);

        HashMap<String, String> customData0 = new HashMap<>();

        if (o.has("custom")) {
            JsonObject o1 = o.get("custom").getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : o1.entrySet())
                customData0.put(e.getKey(), e.getValue().getAsString());
        }

        customData = Collections.unmodifiableMap(customData0);
    }

    public JsonObject toJsonObject() {
        JsonObject o = new JsonObject();

        JsonArray a = new JsonArray();
        for (GameMode m : modes.values()) a.add(new JsonPrimitive(m.getID()));
        o.add("modes", a);

        o.add("default", new JsonPrimitive(defaultMode.getID()));
        o.add("common", new JsonPrimitive(commonMode.getID()));

        if (!customData.isEmpty()) {
            JsonObject o1 = new JsonObject();
            for (Map.Entry<String, String> e : customData.entrySet())
                o1.add(e.getKey(), new JsonPrimitive(e.getValue()));
            o.add("custom", o1);
        }

        return o;
    }

    // Static //

    private static GameModes gameModes = null;

    public static void reload() {
        File file = LMFileUtils.newFile(new File(ServerUtilitiesLib.folderModpack, "gamemodes.json"));
        gameModes = new GameModes(LMJsonUtils.fromJson(file));
        LMJsonUtils.toJson(file, gameModes.toJsonObject());
    }

    public static GameModes getGameModes() {
        if (gameModes == null) reload();
        return gameModes;
    }

    public GameMode get(String s) {
        if (s == null || s.isEmpty()) return defaultMode;
        GameMode m = modes.get(s);
        return (m == null) ? defaultMode : m;
    }
}
