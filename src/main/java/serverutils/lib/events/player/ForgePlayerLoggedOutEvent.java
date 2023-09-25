package serverutils.lib.events.player;

import serverutils.lib.lib.data.ForgePlayer;

public class ForgePlayerLoggedOutEvent extends ForgePlayerEvent {

	public ForgePlayerLoggedOutEvent(ForgePlayer player) {
		super(player);
	}
}