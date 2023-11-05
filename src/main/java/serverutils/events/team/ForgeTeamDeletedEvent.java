package serverutils.events.team;

import java.io.File;

import serverutils.lib.data.ForgeTeam;

public class ForgeTeamDeletedEvent extends ForgeTeamEvent {

    private final File folder;

    public ForgeTeamDeletedEvent(ForgeTeam team, File f) {
        super(team);
        folder = f;
    }

    public File getDataFolder() {
        return folder;
    }
}
