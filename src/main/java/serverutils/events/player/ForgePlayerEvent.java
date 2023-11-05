package serverutils.events.player;

import serverutils.events.team.ForgeTeamEvent;
import serverutils.lib.data.ForgePlayer;

public abstract class ForgePlayerEvent extends ForgeTeamEvent {

    private final ForgePlayer player;

    public ForgePlayerEvent(ForgePlayer p) {
        super(p.team);
        player = p;
    }

    public ForgePlayer getPlayer() {
        return player;
    }
}
