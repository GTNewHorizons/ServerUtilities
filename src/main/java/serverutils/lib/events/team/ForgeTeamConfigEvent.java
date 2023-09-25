package serverutils.lib.events.team;

import serverutils.lib.lib.config.ConfigGroup;
import serverutils.lib.lib.data.ForgeTeam;


public class ForgeTeamConfigEvent extends ForgeTeamEvent {

	private final ConfigGroup config;

	public ForgeTeamConfigEvent(ForgeTeam team, ConfigGroup s) {
		super(team);
		config = s;
	}

	public ConfigGroup getConfig() {
		return config;
	}
}