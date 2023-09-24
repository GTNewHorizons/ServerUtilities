package serverutils.serverlib.events.player;

import serverutils.serverlib.lib.data.ForgePlayer;

public class ForgePlayerLoggedInEvent extends ForgePlayerEvent {

	public ForgePlayerLoggedInEvent(ForgePlayer player) {
		super(player);
	}
}