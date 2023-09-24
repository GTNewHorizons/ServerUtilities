package serverutils.serverlib.net;

import serverutils.serverlib.lib.net.NetworkWrapper;

public class ServerLibNetHandler {
	static final NetworkWrapper GENERAL = NetworkWrapper.newWrapper("ftblib");
	static final NetworkWrapper EDIT_CONFIG = NetworkWrapper.newWrapper("ftblib_edit_config");
	static final NetworkWrapper MY_TEAM = NetworkWrapper.newWrapper("ftblib_my_team");

	public static void init() {
		GENERAL.register(new MessageSyncData());
		GENERAL.registerBlank();
		GENERAL.register(new MessageCloseGui());
		GENERAL.register(new MessageAdminPanelGui());
		GENERAL.register(new MessageAdminPanelGuiResponse());
		GENERAL.register(new MessageAdminPanelAction());

		EDIT_CONFIG.register(new MessageEditConfig());
		EDIT_CONFIG.register(new MessageEditConfigResponse());

		MY_TEAM.register(new MessageSelectTeamGui());
		MY_TEAM.register(new MessageMyTeamGui());
		MY_TEAM.register(new MessageMyTeamGuiResponse());
		MY_TEAM.register(new MessageMyTeamAction());
		MY_TEAM.register(new MessageMyTeamPlayerList());
	}
}