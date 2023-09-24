package serverutils.serverlib.events.team;

import serverutils.serverlib.events.player.ForgePlayerEvent;
import serverutils.serverlib.lib.data.ForgePlayer;

/**
 * @author LatvianModder
 */
public class ForgeTeamPlayerLeftEvent extends ForgePlayerEvent
{
	public ForgeTeamPlayerLeftEvent(ForgePlayer player)
	{
		super(player);
	}
}