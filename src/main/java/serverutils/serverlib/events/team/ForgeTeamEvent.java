package serverutils.serverlib.events.team;

import java.util.Objects;

import serverutils.serverlib.events.universe.UniverseEvent;
import serverutils.serverlib.lib.data.ForgeTeam;

public class ForgeTeamEvent extends UniverseEvent {

	private final ForgeTeam team;

	public ForgeTeamEvent(ForgeTeam t) {
		super(t.universe);
		team = Objects.requireNonNull(t, "Null ForgeTeam in ForgeTeamEvent!");
	}

	public ForgeTeam getTeam() {
		return team;
	}
}
