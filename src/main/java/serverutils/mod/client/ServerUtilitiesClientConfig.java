package serverutils.mod.client;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import serverutils.lib.client.EnumSidebarButtonPlacement;
import serverutils.lib.lib.math.Ticks;

public class ServerUtilitiesClientConfig {

    public static Configuration config;

    public static final String CLIENT_LANG_KEY = "serverutilities_client.";

    public static void init(FMLPreInitializationEvent event) {
        config = new Configuration(
                new File(event.getModConfigurationDirectory() + "/../server utilities/client/serverutilities.cfg"));
        config.load();
        sync();
    }

    public static boolean sync() {
        config.setCategoryLanguageKey(Configuration.CATEGORY_GENERAL, CLIENT_LANG_KEY + "general");
        item_ore_names = config
                .get(
                        Configuration.CATEGORY_GENERAL,
                        "item_ore_names",
                        false,
                        "Show item Ore Dictionary names in inventory.")
                .setLanguageKey(CLIENT_LANG_KEY + "item_ore_names").getBoolean();
        item_nbt = config.get(Configuration.CATEGORY_GENERAL, "item_nbt", false, "Show item NBT in inventory.")
                .setLanguageKey(CLIENT_LANG_KEY + "item_nbt").getBoolean();
        sidebar_buttons = EnumSidebarButtonPlacement.string2placement(
                config.get(
                        Configuration.CATEGORY_GENERAL,
                        "sidebar_buttons",
                        "auto",
                        "DISABLED: Buttons are hidden;"
                                + "\nTOP_LEFT: Buttons are placed on top-left corner, where NEI has it's buttons;"
                                + "\nINVENTORY_SIDE: Buttons are placed on the left side of your inventory,;"
                                + "\nAUTO: When NEI is installed, INVENTORY_SIDE, else TOP_LEFT.")
                        .setLanguageKey(CLIENT_LANG_KEY + "sidebar_buttons").setValidValues(sidebar_buttons_locations)
                        .getString());
        sidebar_buttons_above_potion = config.get(
                Configuration.CATEGORY_GENERAL,
                "sidebar_buttons_above_potion",
                false,
                "Move buttons above potion effect label whenever an effect is active and placement is set to AUTO.")
                .setLanguageKey(CLIENT_LANG_KEY + "sidebar_buttons_above_potion").getBoolean();
        general.show_shutdown_timer_ms = -1L;
        // general.render_badges = config.get(Configuration.CATEGORY_GENERAL, "render_badges", false, "Render
        // badges.").getBoolean();
        // general.journeymap_overlay = config.get(
        // Configuration.CATEGORY_GENERAL,
        // "journeymap_overlay",
        // false,
        // "Enable JourneyMap overlay. Requires a restart to work.").getBoolean();
        general.show_shutdown_timer = config
                .get(
                        Configuration.CATEGORY_GENERAL,
                        "show_shutdown_timer",
                        true,
                        "Show when server will shut down in corner.")
                .setLanguageKey(CLIENT_LANG_KEY + "show_shutdown_timer").getBoolean();
        general.shutdown_timer_start = config
                .get(
                        Configuration.CATEGORY_GENERAL,
                        "shutdown_timer_start",
                        "1m",
                        "When will it start to show the shutdown timer.")
                .setLanguageKey(CLIENT_LANG_KEY + "shutdown_timer_start").getString();
        general.button_daytime = config.get(Configuration.CATEGORY_GENERAL, "button_daytime", 6000, "", 0, 23999)
                .setLanguageKey(CLIENT_LANG_KEY + "button_daytime").getInt();
        general.button_nighttime = config.get(Configuration.CATEGORY_GENERAL, "button_nighttime", 18000, "", 0, 239999)
                .setLanguageKey(CLIENT_LANG_KEY + "button_nighttime").getInt();

        config.save();

        return true;
    }

    public static final General general = new General();

    public static boolean item_ore_names;
    public static boolean item_nbt;
    public static EnumSidebarButtonPlacement sidebar_buttons;
    public static boolean sidebar_buttons_above_potion;
    public static String[] sidebar_buttons_locations = { "DISABLED", "TOP_LEFT", "INVENTORY_SIDE", "AUTO" };

    public static class General {

        public boolean render_badges = false;
        // public boolean journeymap_overlay;
        public boolean show_shutdown_timer;
        public String shutdown_timer_start;
        private long show_shutdown_timer_ms = -1L;

        public long getShowShutdownTimer() {
            if (show_shutdown_timer_ms == -1L) {
                show_shutdown_timer_ms = Ticks.get(shutdown_timer_start).millis();
            }

            return show_shutdown_timer_ms;
        }

        public int button_daytime;
        public int button_nighttime;
    }
}
