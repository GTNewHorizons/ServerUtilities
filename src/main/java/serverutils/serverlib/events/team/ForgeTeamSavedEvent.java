package serverutils.serverlib.events.team;

import serverutils.serverlib.lib.data.ForgeTeam;

/**
 * @author LatvianModder
 */
public class ForgeTeamSavedEvent extends ForgeTeamEvent
{
	public ForgeTeamSavedEvent(ForgeTeam team)
	{
		super(team);
	}
}