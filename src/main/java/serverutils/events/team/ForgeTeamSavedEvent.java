package serverutils.events.team;

import serverutils.lib.data.ForgeTeam;

public class ForgeTeamSavedEvent extends ForgeTeamEvent {

    private boolean successful = true;

    public void markFailed() {
        successful = false;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public ForgeTeamSavedEvent(ForgeTeam team) {
        super(team);
    }
}
