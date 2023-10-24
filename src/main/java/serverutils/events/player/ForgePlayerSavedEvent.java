package serverutils.events.player;

import serverutils.lib.data.ForgePlayer;

public class ForgePlayerSavedEvent extends ForgePlayerEvent {

    public ForgePlayerSavedEvent(ForgePlayer player) {
        super(player);
    }
}
