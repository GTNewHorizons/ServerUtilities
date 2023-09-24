package serverutils.serverlib.events.player;

import serverutils.serverlib.lib.data.ForgePlayer;

public class ForgePlayerLoggedOutEvent extends ForgePlayerEvent {
	public ForgePlayerLoggedOutEvent(ForgePlayer player)
	{
		super(player);
	}
}