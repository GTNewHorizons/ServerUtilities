package serverutils.aurora;

import com.gtnewhorizon.gtnhlib.config.Config;

import serverutils.ServerUtilities;

@Config(modid = ServerUtilities.MOD_ID, category = "", filename = "aurora", configSubDirectory = "../serverutilities/")
public class AuroraConfig {

    public static final General general = new General();
    public static final Pages pages = new Pages();

    public static class General {

        @Config.Comment("Enable the localhost server")
        @Config.DefaultBoolean(false)
        public boolean enable;

        @Config.Comment("Webserver Port ID")
        @Config.DefaultInt(48574)
        @Config.RangeInt(min = 1024, max = 65535)
        public int port;
    }

    public static class Pages {

        @Config.Comment("Enable the modlist page")
        @Config.DefaultEnum("ENABLED")
        public PageType modlist_page;

        @Config.Comment("Enable the modlist page")
        @Config.DefaultEnum("ENABLED")
        public PageType world_info_json;

        @Config.Comment("Enable the world info page")
        @Config.DefaultEnum("ENABLED")
        public PageType player_list_table;

        @Config.Comment("Enable the playerlist table page")
        @Config.DefaultEnum("ENABLED")
        public PageType player_list_json;

        @Config.Comment("Enable the playerlist json page")
        @Config.DefaultEnum("REQUIRES_AUTH")
        public PageType player_rank_page;

        @Config.Comment("Enable the player rank page from Server Utilities")
        @Config.DefaultEnum("ENABLED")
        public PageType command_list_page;

        @Config.Comment("Enable the command list page from Server Utilities")
        @Config.DefaultEnum("ENABLED")
        public PageType permission_list_page;

        @Config.Comment("Exclude mods from the modlist")
        @Config.DefaultStringList({})
        public String[] modlist_excluded_mods;
    }
}
