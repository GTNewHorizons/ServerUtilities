package serverutils.lib.events.team;

import serverutils.lib.lib.data.ForgeTeam;

public class ForgeTeamCreatedEvent extends ForgeTeamEvent {

    public ForgeTeamCreatedEvent(ForgeTeam team) {
        super(team);
    }
}
