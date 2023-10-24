package serverutils.events.team;

import serverutils.lib.data.ForgeTeam;

public class ForgeTeamCreatedEvent extends ForgeTeamEvent {

    public ForgeTeamCreatedEvent(ForgeTeam team) {
        super(team);
    }
}
