package serverutils.mod.client;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import serverutils.lib.client.EnumSidebarButtonPlacement;
import serverutils.lib.lib.math.Ticks;
import serverutils.mod.ServerUtilities;

public class ServerUtilitiesClientConfig {

    public static Configuration config;

    public static final ServerUtilitiesClientConfig INST = new ServerUtilitiesClientConfig();

    public static void init(FMLPreInitializationEvent event) {
        config = new Configuration(
                new File(event.getModConfigurationDirectory() + "/../server utilities/client/serverutilities.cfg"));
        sync();
    }

    public static boolean sync() {

        config.load();

        item_ore_names = config.get(
                Configuration.CATEGORY_GENERAL,
                "item_ore_names",
                false,
                "Show item Ore Dictionary names in inventory.").getBoolean();

        item_nbt = config.get(Configuration.CATEGORY_GENERAL, "item_nbt", false, "Show item NBT in inventory.")
                .getBoolean();

        debug_helper = config
                .get(Configuration.CATEGORY_GENERAL, "debug_helper", true, "Show help text while holding F3.")
                .getBoolean();

        replace_vanilla_status_messages = config
                .get(
                        Configuration.CATEGORY_GENERAL,
                        "replace_vanilla_status_messages",
                        true,
                        "Replace vanilla status message with Notifications, which support colors and timers.")
                .getBoolean();

        action_buttons = EnumSidebarButtonPlacement.string2placement(
                config.get(
                        Configuration.CATEGORY_GENERAL,
                        "action_buttons",
                        "auto",
                        "DISABLED: Buttons are hidden;\nTOP_LEFT: Buttons are placed on top-left corner, where NEI has it's buttons;\nINVENTORY_SIDE: Buttons are placed on the side or top of your inventory, depending on potion effects and crafting book;\nAUTO: When NEI is installed, INVENTORY_SIDE, else TOP_LEFT.")
                        .getString());
        general.show_shutdown_timer_ms = -1L;
        // general.render_badges = config.get(Configuration.CATEGORY_GENERAL, "render_badges", false, "Render
        // badges.").getBoolean();
        // general.journeymap_overlay = config.get(
        // Configuration.CATEGORY_GENERAL,
        // "journeymap_overlay",
        // false,
        // "Enable JourneyMap overlay. Requires a restart to work.").getBoolean();
        general.show_shutdown_timer = config.get(
                Configuration.CATEGORY_GENERAL,
                "show_shutdown_timer",
                true,
                "Show when server will shut down in corner.").getBoolean();
        general.shutdown_timer_start = config.get(
                Configuration.CATEGORY_GENERAL,
                "shutdown_timer_start",
                "1m",
                "When will it start to show the shutdown timer.").getString();
        general.button_daytime = config.get(Configuration.CATEGORY_GENERAL, "button_daytime", 6000, "", 0, 23999)
                .getInt();
        general.button_nighttime = config.get(Configuration.CATEGORY_GENERAL, "button_nighttime", 18000, "", 0, 239999)
                .getInt();
        config.save();

        return true;
    }

    public static final General general = new General();

    public static boolean item_ore_names;
    public static boolean item_nbt;
    public static EnumSidebarButtonPlacement action_buttons;
    public static boolean replace_vanilla_status_messages;
    public static boolean debug_helper;

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

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.modID.equals(ServerUtilities.MOD_ID)) {
            sync();
        }
    }

}
