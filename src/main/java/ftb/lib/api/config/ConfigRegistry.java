package ftb.lib.api.config;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;

import com.google.gson.JsonElement;

import ftb.lib.FTBLib;
import ftb.lib.LMAccessToken;
import ftb.lib.mod.net.MessageEditConfig;
import latmod.lib.LMJsonUtils;
import latmod.lib.LMUtils;

public class ConfigRegistry {

    public static final HashMap<String, ConfigFile> map = new HashMap<>();
    public static final ConfigGroup synced = new ConfigGroup("synced");
    private static final HashMap<String, ConfigFile> tempServerConfig = new HashMap<>();

    public static void add(ConfigFile f) {
        if (f != null) {
            map.put(f.getID(), f);

            ConfigGroup g1 = f.generateSynced(false);
            if (!g1.entryMap.isEmpty()) synced.add(g1, false);
        }
    }

    public static void reload() {
        for (ConfigFile f : map.values()) f.load();

        FTBLib.dev_logger.info("Loading override configs");
        JsonElement overridesE = LMJsonUtils.fromJson(new File(FTBLib.folderModpack, "overrides.json"));

        if (overridesE.isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : overridesE.getAsJsonObject().entrySet()) {
                ConfigGroup ol = new ConfigGroup(e.getKey());
                ol.func_152753_a(e.getValue());

                int result;
                ConfigFile f = map.get(ol.getID());
                if (f != null && (result = f.loadFromGroup(ol, false)) > 0) {
                    FTBLib.dev_logger.info("Config '" + e.getKey() + "' overriden: " + result);
                    f.save();
                } else FTBLib.dev_logger.info("Didnt load anything from " + e.getKey());
            }
        }

        for (ConfigFile f : map.values()) f.save();
    }

    public static ConfigFile createTempConfig(EntityPlayerMP ep) {
        if (ep != null) {
            ConfigFile group = new ConfigFile(LMUtils.fromUUID(ep.getGameProfile().getId()));
            tempServerConfig.put(group.getID(), group);
            return group;
        }

        return null;
    }

    public static void editTempConfig(EntityPlayerMP ep, ConfigFile file, boolean reload) {
        if (ep != null && file != null && tempServerConfig.containsValue(file))
            new MessageEditConfig(LMAccessToken.generate(ep), reload, file).sendTo(ep);
    }

    public static ConfigFile getTempConfig(String id) {
        ConfigFile group = tempServerConfig.get(id);
        if (group != null) tempServerConfig.remove(id);
        return group;
    }

    public static void clearTemp() {
        tempServerConfig.clear();
    }
}
