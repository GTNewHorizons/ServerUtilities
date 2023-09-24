package serverutils.serverlib.events.player;

import serverutils.serverlib.events.team.ForgeTeamEvent;
import serverutils.serverlib.lib.data.ForgePlayer;

/**
 * @author LatvianModder
 */
public abstract class ForgePlayerEvent extends ForgeTeamEvent {
	private final ForgePlayer player;

	public ForgePlayerEvent(ForgePlayer p) {
		super(p.team);
		player = p;
	}

	public ForgePlayer getPlayer()
	{
		return player;
	}
}