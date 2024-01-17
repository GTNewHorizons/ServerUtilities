package serverutils.aurora;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class AuroraConfig {

    public static Configuration config;
    public static final String GEN_CAT = Configuration.CATEGORY_GENERAL;
    public static final String PAGES_CAT = "Pages";

    public static void init(FMLPreInitializationEvent event) {
        config = new Configuration(new File(event.getModConfigurationDirectory() + "/../serverutilities/aurora.cfg"));
        sync();
    }

    public static boolean sync() {
        config.load();

        general.enable = config.get(GEN_CAT, "enable", false, "Enable the localhost server, Default: false")
                .getBoolean();
        general.port = config.get(GEN_CAT, "port", 48574, "Webserver Port ID, Default: 48574", 1025, 65534).getInt();
        general.modlist_page = config
                .get(
                        PAGES_CAT,
                        "modlist_page",
                        "ENABLED",
                        "Enable the modlist page, Default: ENABLED, Valid values: ENABLED, REQUIRES_AUTH, DISABLED")
                .getString();
        general.world_info_json = config
                .get(
                        PAGES_CAT,
                        "world_info_json",
                        "ENABLED",
                        "Enable the world info page, Default: ENABLED, Valid values: ENABLED, REQUIRES_AUTH, DISABLED")
                .getString();
        general.player_list_table = config.get(
                PAGES_CAT,
                "player_list_table",
                "ENABLED",
                "Enable the playerlist table page, Default: ENABLED, Valid values: ENABLED, REQUIRES_AUTH, DISABLED")
                .getString();
        general.player_list_json = config.get(
                PAGES_CAT,
                "player_list_json",
                "ENABLED",
                "Enable the playerlist json page, Default: ENABLED, Valid values: ENABLED, REQUIRES_AUTH, DISABLED")
                .getString();
        general.player_rank_page = config.get(
                PAGES_CAT,
                "player_rank_page",
                "REQUIRES_AUTH",
                "Enable the player rank page from Server Utilities, Default: REQUIRES_AUTH, Valid values: ENABLED, REQUIRES_AUTH, DISABLED")
                .getString();
        general.command_list_page = config.get(
                PAGES_CAT,
                "command_list_page",
                "ENABLED",
                "Enable the command list page from Server Utilities, Default: ENABLED, Valid values: ENABLED, REQUIRES_AUTH, DISABLED")
                .getString();
        general.permission_list_page = config.get(
                PAGES_CAT,
                "permission_list_page",
                "ENABLED",
                "Enable the permission list page from Server Utilities, Default: ENABLED, Valid values: ENABLED, REQUIRES_AUTH, DISABLED")
                .getString();

        general.modlist_excluded_mods = config.get(
                PAGES_CAT,
                "modlist_excluded_mods",
                new String[] {},
                "Exclude mods from the modlist: Default: Empty").getStringList();

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
        public String player_rank_page;
        public String command_list_page;
        public String permission_list_page;
        public String[] modlist_excluded_mods;
        public int port;

    }
}
