package serverutils.utils.client;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import com.feed_the_beast.ftblib.lib.math.Ticks;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * @author LatvianModder
 */
public class ServerUtilitiesClientConfig {

    public static Configuration config;

    public static void init(FMLPreInitializationEvent event) {
        config = new Configuration(
                new File(event.getModConfigurationDirectory() + "/../local/client/ftbutilities.cfg"));
        sync();
    }

    public static boolean sync() {

        config.load();
        general.show_shutdown_timer_ms = -1L;
        // general.render_badges = config.get(Configuration.CATEGORY_GENERAL, "render_badges", false, "Render
        // badges.").getBoolean();
        general.journeymap_overlay = config.get(
                Configuration.CATEGORY_GENERAL,
                "journeymap_overlay",
                false,
                "Enable JourneyMap overlay. Requires a restart to work.").getBoolean();
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

    public static class General {

        public boolean render_badges = false;
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

}
