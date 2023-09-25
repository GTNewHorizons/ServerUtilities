package serverutils.lib.events.player;

import serverutils.lib.lib.data.ForgePlayer;

public class ForgePlayerSavedEvent extends ForgePlayerEvent {

    public ForgePlayerSavedEvent(ForgePlayer player) {
        super(player);
    }
}
