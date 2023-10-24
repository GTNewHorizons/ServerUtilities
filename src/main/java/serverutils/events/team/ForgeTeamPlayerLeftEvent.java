package serverutils.events.team;

import serverutils.events.player.ForgePlayerEvent;
import serverutils.lib.data.ForgePlayer;

public class ForgeTeamPlayerLeftEvent extends ForgePlayerEvent {

    public ForgeTeamPlayerLeftEvent(ForgePlayer player) {
        super(player);
    }
}
