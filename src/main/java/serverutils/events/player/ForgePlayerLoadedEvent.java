package serverutils.events.player;

import serverutils.lib.data.ForgePlayer;

public class ForgePlayerLoadedEvent extends ForgePlayerEvent {

    public ForgePlayerLoadedEvent(ForgePlayer player) {
        super(player);
    }
}
