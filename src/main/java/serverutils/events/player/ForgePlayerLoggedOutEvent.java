package serverutils.events.player;

import serverutils.lib.data.ForgePlayer;

public class ForgePlayerLoggedOutEvent extends ForgePlayerEvent {

    public ForgePlayerLoggedOutEvent(ForgePlayer player) {
        super(player);
    }
}
