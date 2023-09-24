package serverutils.serverlib.events.player;

import serverutils.serverlib.lib.data.ForgePlayer;

public class ForgePlayerLoadedEvent extends ForgePlayerEvent {
	public ForgePlayerLoadedEvent(ForgePlayer player)
	{
		super(player);
	}
}