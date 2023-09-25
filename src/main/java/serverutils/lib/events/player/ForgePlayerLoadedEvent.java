package serverutils.lib.events.player;

import serverutils.lib.lib.data.ForgePlayer;

public class ForgePlayerLoadedEvent extends ForgePlayerEvent {

	public ForgePlayerLoadedEvent(ForgePlayer player) {
		super(player);
	}
}