package serverutils.events.team;

import serverutils.lib.data.ForgeTeam;

public class ForgeTeamLoadedEvent extends ForgeTeamEvent {

    public ForgeTeamLoadedEvent(ForgeTeam team) {
        super(team);
    }
}
