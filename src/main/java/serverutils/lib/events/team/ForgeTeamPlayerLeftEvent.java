package serverutils.lib.events.team;

import serverutils.lib.lib.data.ForgePlayer;
import serverutils.lib.events.player.ForgePlayerEvent;

public class ForgeTeamPlayerLeftEvent extends ForgePlayerEvent {

	public ForgeTeamPlayerLeftEvent(ForgePlayer player) {
		super(player);
	}
}