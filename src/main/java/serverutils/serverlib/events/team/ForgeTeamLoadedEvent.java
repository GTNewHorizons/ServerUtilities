package serverutils.serverlib.events.team;

import serverutils.serverlib.lib.data.ForgeTeam;

public class ForgeTeamLoadedEvent extends ForgeTeamEvent
{
	public ForgeTeamLoadedEvent(ForgeTeam team)
	{
		super(team);
	}
}