package serverutils.serverlib;

import net.minecraftforge.common.config.Configuration;

import serverutils.serverlib.lib.config.EnumTristate;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class ServerLibConfig {

	public static Configuration config;
	public static final ServerLibConfig INST = new ServerLibConfig();
	
	public static final String GEN_CAT = Configuration.CATEGORY_GENERAL;
    public static final String TEAM_CAT = "team";
    public static final String DEBUG_CAT = "debugging";
	
	public static void init(FMLPreInitializationEvent event) {
        config = new Configuration(event.getSuggestedConfigurationFile());
        sync();
    }
	
	public static boolean sync() {
        config.load();

		general.clientless_mode = config.get(
                GEN_CAT,
                "clientless_mode",
                false,
                "When this mode is enabled, ServerLib assumes that server clients don't have ServerLib and/or other mods installed.")
                .getBoolean();
        general.replace_reload_command = config
                .get(GEN_CAT, "replace_reload_command", true, "This will replace /reload with Serverlib version of it.")
                .getBoolean();
        general.merge_offline_mode_players = EnumTristate.string2tristate(
                config.get(
                        GEN_CAT,
                        "merge_offline_mode_players",
                        EnumTristate.TRUE.getName(),
                        "Merges player profiles, in case player logged in without internet connection/in offline mode server. If set to DEFAULT, it will only merge on singleplayer worlds.")
                        .getString());
        config.setCategoryRequiresWorldRestart(GEN_CAT, true);
		
		teams.disable_teams = config.get(TEAM_CAT, "disable_teams", false).getBoolean();
		
		teams.autocreate_mp = config.get(
                TEAM_CAT,
                "autocreate_mp",
                false,
                "Automatically creates a team for player on multiplayer, based on their username and with a random color.")
                .getBoolean();
        teams.autocreate_sp = config
                .get(
                        TEAM_CAT,
                        "autocreate_sp",
                        true,
                        "Automatically creates (or joins) a team on singleplayer/LAN with ID 'singleplayer'.")
                .getBoolean();
        teams.hide_team_notification = config
                .get(TEAM_CAT, "hide_team_notification", false, "Disable no team notification entirely.").getBoolean();

        config.setCategoryComment(DEBUG_CAT, "Don't set any values to true, unless you are debugging the mod.");
        debugging.special_commands = config.get(DEBUG_CAT, "special_commands", false, "Enables special debug commands.")
                .getBoolean();
        debugging.print_more_info = config.get(DEBUG_CAT, "print_more_info", false, "Print more info.").getBoolean();
        debugging.print_more_errors = config.get(DEBUG_CAT, "print_more_errors", false, "Print more errors.")
                .getBoolean();
        debugging.log_network = config
                .get(DEBUG_CAT, "log_network", false, "Log incoming and outgoing network messages.").getBoolean();
        debugging.log_teleport = config.get(DEBUG_CAT, "log_teleport", false, "Log player teleporting.").getBoolean();
        debugging.log_config_editing = config.get(DEBUG_CAT, "log_config_editing", false, "Log config editing.")
                .getBoolean();
        debugging.dev_sidebar_buttons = config.get(
                DEBUG_CAT,
                "dev_sidebar_buttons",
                false,
                "See dev-only sidebar buttons. They probably don't do anything.").getBoolean();
        debugging.gui_widget_bounds = config
                .get(DEBUG_CAT, "gui_widget_bounds", false, "See GUI widget bounds when you hold B.").getBoolean();
        debugging.log_events = config.get(DEBUG_CAT, "log_events", false, "Log all events that extend EventBase.")
                .getBoolean();

        config.save();
		
		return true;
    }
	public static final General general = new General();
    public static final Teams teams = new Teams();
    public static final Debugging debugging = new Debugging();

	public static class General {
        public boolean clientless_mode;
        public boolean replace_reload_command;
        public EnumTristate merge_offline_mode_players;
    }

	public static class Teams {
        public boolean disable_teams;
        public boolean autocreate_mp;
        public boolean autocreate_sp;
        public boolean hide_team_notification;
    }

    public static class Debugging {
        public boolean special_commands;
        public boolean print_more_info;
        public boolean print_more_errors;
        public boolean log_network;
        public boolean log_teleport;
        public boolean log_config_editing;
        public boolean dev_sidebar_buttons;
        public boolean gui_widget_bounds;
        public boolean log_events;
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.modID.equals(ServerLib.MOD_ID)) {
            sync();
        }
    }

}