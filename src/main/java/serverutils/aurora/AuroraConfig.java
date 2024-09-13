package serverutils.aurora;

import com.gtnewhorizon.gtnhlib.config.Config;

@Config(modid = "serverutilities", filename = "aurora", configSubDirectory = "../serverutilities/")
public class AuroraConfig {

    @Config.Comment("Enable the localhost server")
    @Config.DefaultBoolean(false)
    public static boolean enable;

    @Config.Comment("Enable the modlist page")
    @Config.DefaultEnum("ENABLED")
    public static PageType modlist_page;

    @Config.Comment("Enable the modlist page")
    @Config.DefaultEnum("ENABLED")
    public static PageType world_info_json;

    @Config.Comment("Enable the world info page")
    @Config.DefaultEnum("ENABLED")
    public static PageType player_list_table;

    @Config.Comment("Enable the playerlist table page")
    @Config.DefaultEnum("ENABLED")
    public static PageType player_list_json;

    @Config.Comment("Enable the playerlist json page")
    @Config.DefaultEnum("REQUIRES_AUTH")
    public static PageType player_rank_page;

    @Config.Comment("Enable the player rank page from Server Utilities")
    @Config.DefaultEnum("ENABLED")
    public static PageType command_list_page;

    @Config.Comment("Enable the command list page from Server Utilities")
    @Config.DefaultEnum("ENABLED")
    public static PageType permission_list_page;

    @Config.Comment("Exclude mods from the modlist")
    @Config.DefaultStringList({})
    public static String[] modlist_excluded_mods;

    @Config.Comment("Webserver Port ID")
    @Config.DefaultInt(48574)
    public static int port;
}
