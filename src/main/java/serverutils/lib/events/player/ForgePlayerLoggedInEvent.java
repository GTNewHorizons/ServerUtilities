package serverutils.lib.events.player;

import serverutils.lib.lib.data.ForgePlayer;

public class ForgePlayerLoggedInEvent extends ForgePlayerEvent {

    public ForgePlayerLoggedInEvent(ForgePlayer player) {
        super(player);
    }
}
