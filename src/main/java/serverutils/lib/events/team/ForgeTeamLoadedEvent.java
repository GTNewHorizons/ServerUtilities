package serverutils.lib.events.team;

import serverutils.lib.lib.data.ForgeTeam;

public class ForgeTeamLoadedEvent extends ForgeTeamEvent {

    public ForgeTeamLoadedEvent(ForgeTeam team) {
        super(team);
    }
}
