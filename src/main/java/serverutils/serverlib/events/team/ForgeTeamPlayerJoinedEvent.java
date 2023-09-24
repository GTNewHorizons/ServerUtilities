package serverutils.serverlib.events.team;

import javax.annotation.Nullable;

import serverutils.serverlib.events.player.ForgePlayerEvent;
import serverutils.serverlib.lib.data.ForgePlayer;

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