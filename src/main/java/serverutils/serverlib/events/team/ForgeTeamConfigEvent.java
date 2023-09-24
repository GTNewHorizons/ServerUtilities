package serverutils.serverlib.events.team;

import serverutils.serverlib.lib.config.ConfigGroup;
import serverutils.serverlib.lib.data.ForgeTeam;


public class ForgeTeamConfigEvent extends ForgeTeamEvent {
	private final ConfigGroup config;

	public ForgeTeamConfigEvent(ForgeTeam team, ConfigGroup s) {
		super(team);
		config = s;
	}

	public ConfigGroup getConfig()
	{
		return config;
	}
}