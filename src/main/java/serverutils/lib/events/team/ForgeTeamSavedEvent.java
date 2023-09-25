package serverutils.lib.events.team;

import serverutils.lib.lib.data.ForgeTeam;

public class ForgeTeamSavedEvent extends ForgeTeamEvent {

	public ForgeTeamSavedEvent(ForgeTeam team) {
		super(team);
	}
}