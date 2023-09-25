package serverutils.lib.events.player;

import serverutils.lib.events.team.ForgeTeamEvent;
import serverutils.lib.lib.data.ForgePlayer;

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
