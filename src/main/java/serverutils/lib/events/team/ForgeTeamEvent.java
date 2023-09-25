package serverutils.lib.events.team;

import java.util.Objects;

import serverutils.lib.events.universe.UniverseEvent;
import serverutils.lib.lib.data.ForgeTeam;

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
