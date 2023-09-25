package serverutils.lib.events.team;

import serverutils.lib.lib.data.ForgeTeam;
import serverutils.lib.lib.data.NBTDataStorage;

import java.util.function.Consumer;

public class ForgeTeamDataEvent extends ForgeTeamEvent {

	private final Consumer<NBTDataStorage.Data> callback;

	public ForgeTeamDataEvent(ForgeTeam team, Consumer<NBTDataStorage.Data> c) {
		super(team);
		callback = c;
	}

	public void register(NBTDataStorage.Data data) {
		callback.accept(data);
	}
}