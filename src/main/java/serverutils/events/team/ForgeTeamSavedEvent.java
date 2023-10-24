package serverutils.events.team;

import serverutils.lib.data.ForgeTeam;

public class ForgeTeamSavedEvent extends ForgeTeamEvent {

    public ForgeTeamSavedEvent(ForgeTeam team) {
        super(team);
    }
}
