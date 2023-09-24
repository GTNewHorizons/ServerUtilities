package serverutils.serverlib.events.team;

import serverutils.serverlib.lib.data.ForgeTeam;

public class ForgeTeamCreatedEvent extends ForgeTeamEvent {

	public ForgeTeamCreatedEvent(ForgeTeam team) {
		super(team);
	}
}