package serverutils.lib.events.team;

import serverutils.lib.events.player.ForgePlayerEvent;
import serverutils.lib.lib.data.ForgePlayer;

public class ForgeTeamPlayerLeftEvent extends ForgePlayerEvent {

    public ForgeTeamPlayerLeftEvent(ForgePlayer player) {
        super(player);
    }
}
