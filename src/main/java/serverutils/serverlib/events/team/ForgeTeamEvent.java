package serverutils.serverlib.events.team;

import serverutils.serverlib.events.universe.UniverseEvent;
import serverutils.serverlib.lib.data.ForgeTeam;

import java.util.Objects;

public class ForgeTeamEvent extends UniverseEvent
{
	private final ForgeTeam team;

	public ForgeTeamEvent(ForgeTeam t)
	{
		super(t.universe);
		team = Objects.requireNonNull(t, "Null ForgeTeam in ForgeTeamEvent!");
	}

	public ForgeTeam getTeam()
	{
		return team;
	}
}