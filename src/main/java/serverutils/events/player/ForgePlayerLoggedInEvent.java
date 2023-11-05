package serverutils.events.player;

import serverutils.lib.data.ForgePlayer;

public class ForgePlayerLoggedInEvent extends ForgePlayerEvent {

    public ForgePlayerLoggedInEvent(ForgePlayer player) {
        super(player);
    }
}
