package serverutils.aurora;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class AuroraConfig {

    public static Configuration config;
    public static final AuroraConfig INST = new AuroraConfig();
    public static final String GEN_CAT = Configuration.CATEGORY_GENERAL;

    public static void init(FMLPreInitializationEvent event) {
        config = new Configuration(new File(event.getModConfigurationDirectory() + "/../server utilities/aurora.cfg"));
        sync();
    }

    public static boolean sync() {
        config.load();

        general.enable = config.get(GEN_CAT, "enable", false, "Enable the localhost server, Default: false")
                .getBoolean();
        general.port = config.get(GEN_CAT, "port", 48574, "Webserver Port ID, Default: 48574", 1025, 65534).getInt();
        general.modlist_page = config.get(
                GEN_CAT,
                "modlist_page",
                "ENABLED",
                "Enable the modlist page, Valid values: ENABLED, REQUIRES_AUTH, DISABLED").getString();
        general.world_info_json = config.get(
                GEN_CAT,
                "world_info_json",
                "DISABLED",
                "Enable the world info page, Valid values: ENABLED, REQUIRES_AUTH, DISABLED").getString();;
        general.player_list_table = config.get(
                GEN_CAT,
                "player_list_table",
                "DISABLED",
                "Enable the playerlist table page, Valid values: ENABLED, REQUIRES_AUTH, DISABLED").getString();
        general.player_list_json = config.get(
                GEN_CAT,
                "player_list_json",
                "DISABLED",
                "Enable the playerlist json page, Valid values: ENABLED, REQUIRES_AUTH, DISABLED").getString();

        general.modlist_excluded_mods = config
                .get("modlist_excluded_mods", GEN_CAT, new String[] {}, "Exclude mods from the modlist: Default: Empty")
                .getStringList();

        config.save();
        return true;

    }

    public static final General general = new General();

    public static class General {

        public boolean enable;
        public String modlist_page;
        public String world_info_json;
        public String player_list_table;
        public String player_list_json;
        public String[] modlist_excluded_mods;
        public int port;

    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.modID.equals(Aurora.MOD_ID)) {
            sync();
        }
    }
}
