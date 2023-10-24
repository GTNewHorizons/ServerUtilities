package serverutils.events.team;

import serverutils.lib.data.ForgeTeam;

public class ForgeTeamChangedEvent extends ForgeTeamEvent {

    private final ForgeTeam oldTeam;

    public ForgeTeamChangedEvent(ForgeTeam team, ForgeTeam o) {
        super(team);
        oldTeam = o;
    }

    public ForgeTeam getOldTeam() {
        return oldTeam;
    }
}
