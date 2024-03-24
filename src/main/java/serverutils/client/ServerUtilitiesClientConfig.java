package serverutils.client;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.lib.math.Ticks;

public class ServerUtilitiesClientConfig {

    public static Configuration config;
    public static ServerUtilitiesClientConfig INST = new ServerUtilitiesClientConfig();
    public static final String CLIENT_LANG_KEY = "serverutilities_client.";

    public static void init(FMLPreInitializationEvent event) {
        config = new Configuration(
                new File(
                        event.getModConfigurationDirectory() + "/../"
                                + ServerUtilitiesClient.CLIENT_FOLDER
                                + "serverutilities.cfg"));
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
        sidebar_buttons = EnumSidebarLocation.stringToEnum(
                config.get(Configuration.CATEGORY_GENERAL, "sidebar_buttons", "auto", """
                        DISABLED: Buttons are hidden.
                        TOP_LEFT: Buttons are placed on top-left corner, where NEI has its buttons.
                        INVENTORY_SIDE: Buttons are placed on the left side of your inventory.
                        UNLOCKED: Buttons are able to be dragged around by holding CTRL""")
                        .setLanguageKey(CLIENT_LANG_KEY + "sidebar_buttons").setValidValues(sidebar_buttons_locations)
                        .getString());
        notifications = EnumNotificationLocation.stringToEnum(
                config.get(Configuration.CATEGORY_GENERAL, "notification_location", "SCREEN", """
                        SCREEN: Receive notifications as normal above the hotbar.
                        CHAT: Convert all non-important notifications to chat messages.
                        DISABLED: Disable non-important notifications entirely.""")
                        .setLanguageKey(CLIENT_LANG_KEY + "notification_location")
                        .setValidValues(notification_locations).getString());
        sidebar_buttons_above_potion = config.get(
                Configuration.CATEGORY_GENERAL,
                "sidebar_buttons_above_potion",
                false,
                "Move buttons above potion effect label when sidebar is locked to INVENTORY_SIDE and player has potion effects.")
                .setLanguageKey(CLIENT_LANG_KEY + "sidebar_buttons_above_potion").getBoolean();
        sidebar_placement = EnumPlacement.stringToEnum(
                config.get(Configuration.CATEGORY_GENERAL, "sidebar_placement", "GROUPED", """
                        GROUPED: Buttons are grouped together.
                        VERTICAL: Buttons are placed in a vertical line.
                        HORIZONTAL: Buttons are placed in a horizontal line.""")
                        .setLanguageKey(CLIENT_LANG_KEY + "sidebar_placement").setValidValues(valid_sidebar_placement)
                        .getString());
        show_dotted_lines = config
                .get(
                        Configuration.CATEGORY_GENERAL,
                        "show_dotted_lines",
                        true,
                        "Draw dotted lines on loaded chunks to improve noticeability.")
                .setLanguageKey(CLIENT_LANG_KEY + "show_dotted_lines").getBoolean();
        general.show_shutdown_timer_ms = -1L;
        general.journeymap_overlay = config.get(
                Configuration.CATEGORY_GENERAL,
                "journeymap_overlay",
                true,
                "Enable JourneyMap overlay. Requires VisualProspecting installed. Changes will apply after restart.")
                .getBoolean();
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
    public static EnumSidebarLocation sidebar_buttons;
    public static boolean sidebar_buttons_above_potion;
    public static String[] sidebar_buttons_locations = { "UNLOCKED", "INVENTORY_SIDE", "TOP_LEFT", "DISABLED" };
    public static EnumPlacement sidebar_placement;
    public static String[] valid_sidebar_placement = { "GROUPED", "VERTICAL", "HORIZONTAL" };
    public static EnumNotificationLocation notifications;
    public static String[] notification_locations = { "SCREEN", "CHAT", "DISABLED" };
    public static boolean show_dotted_lines;

    public static class General {

        public boolean journeymap_overlay;
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

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.modID.equals(ServerUtilities.MOD_ID)) {
            ServerUtilitiesClientConfig.sync();
            ServerUtilitiesConfig.sync();
        }
    }
}
