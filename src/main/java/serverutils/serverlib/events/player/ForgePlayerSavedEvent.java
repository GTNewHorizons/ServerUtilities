package serverutils.serverlib.events.player;

import serverutils.serverlib.lib.data.ForgePlayer;

public class ForgePlayerSavedEvent extends ForgePlayerEvent {

	public ForgePlayerSavedEvent(ForgePlayer player) {
		super(player);
	}
}