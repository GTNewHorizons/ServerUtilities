package serverutils.lib.events.team;

import javax.annotation.Nullable;

import serverutils.lib.lib.data.ForgePlayer;
import serverutils.lib.events.player.ForgePlayerEvent;

public class ForgeTeamPlayerJoinedEvent extends ForgePlayerEvent {

	private Runnable displayGui;

	public ForgeTeamPlayerJoinedEvent(ForgePlayer player) {
		super(player);
	}

	public void setDisplayGui(Runnable gui) {
		displayGui = gui;
	}

	@Nullable
	public Runnable getDisplayGui() {
		return displayGui;
	}
}