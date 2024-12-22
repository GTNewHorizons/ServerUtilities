package serverutils.client;

import com.gtnewhorizon.gtnhlib.config.Config;

import serverutils.ServerUtilities;
import serverutils.lib.math.Ticks;

@Config(modid = ServerUtilities.MOD_ID, configSubDirectory = "../serverutilities/client/")
public class ServerUtilitiesClientConfig {

    @Config.Comment("Show item Ore Dictionary names in inventory.")
    @Config.LangKey("serverutilities_client.item_ore_names")
    @Config.DefaultBoolean(false)
    public static boolean item_ore_names;

    @Config.Comment("Show item NBT in inventory.")
    @Config.LangKey("serverutilities_client.item_nbt")
    @Config.DefaultBoolean(false)
    public static boolean item_nbt;

    @Config.Comment("Where to place the sidebar buttons.")
    @Config.LangKey("serverutilities_client.sidebar_buttons")
    @Config.DefaultEnum("UNLOCKED")
    public static EnumSidebarLocation sidebar_buttons;

    @Config.Comment("Move buttons above potion effect label when sidebar is locked to INVENTORY_SIDE and player has potion effects.")
    @Config.LangKey("serverutilities_client.sidebar_buttons_above_potion")
    @Config.DefaultBoolean(false)
    public static boolean sidebar_buttons_above_potion;

    @Config.Comment("""
            GROUPED: Buttons are grouped together.
            VERTICAL: Buttons are placed in a vertical line.
            HORIZONTAL: Buttons are placed in a horizontal line.""")
    @Config.LangKey("serverutilities_client.sidebar_placement")
    @Config.DefaultEnum("GROUPED")
    public static EnumPlacement sidebar_placement;

    @Config.Comment("Draw dotted lines on loaded chunks to improve noticeability.")
    @Config.LangKey("serverutilities_client.show_dotted_lines")
    @Config.DefaultBoolean(true)
    public static boolean show_dotted_lines;

    @Config.Comment("Show when server will shut down in corner.")
    @Config.LangKey("serverutilities_client.show_shutdown_timer")
    @Config.DefaultBoolean(true)
    public static boolean show_shutdown_timer;

    @Config.Comment("When will it start to show the shutdown timer.")
    @Config.LangKey("serverutilities_client.shutdown_timer_start")
    @Config.DefaultString("1m")
    public static String shutdown_timer_start;

    @Config.DefaultInt(6000)
    public static int button_daytime;

    @Config.DefaultInt(18000)
    public static int button_nighttime;

    @Config.Ignore
    private static long show_shutdown_timer_ms = -1L;

    public static long getShowShutdownTimer() {
        if (show_shutdown_timer_ms == -1L) {
            show_shutdown_timer_ms = Ticks.get(shutdown_timer_start).millis();
        }

        return show_shutdown_timer_ms;
    }
}
