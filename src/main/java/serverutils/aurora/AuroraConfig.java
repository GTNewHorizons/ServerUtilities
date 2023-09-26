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
    public static PageType modlist_page = PageType.ENABLED;
    public static PageType world_info_json = PageType.ENABLED;
    public static PageType player_list_table = PageType.ENABLED;
    public static PageType player_list_json = PageType.ENABLED;

    public static String[] modlist_excluded_mods = {};

    public static void init(FMLPreInitializationEvent event) {
        config = new Configuration(new File(event.getModConfigurationDirectory() + "/../server utilities/aurora.cfg"));
        sync();
    }

    public static boolean sync() {
        config.load();

        general.enable = config.get(GEN_CAT, "enable", false, "Enable the localhost server, Default: false")
                .getBoolean();
        general.port = config.get(GEN_CAT, "port", 48574, "Website Port ID, Default: 48574", 1025, 65534).getInt();

        config.save();
        return true;

    }

    public static final General general = new General();

    public static class General {

        public static boolean enable;
        public static int port;

    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.modID.equals(Aurora.MOD_ID)) {
            sync();
        }
    }
}
