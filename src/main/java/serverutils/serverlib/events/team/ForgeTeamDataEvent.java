package serverutils.serverlib.events.team;

import serverutils.serverlib.lib.data.ForgeTeam;
import serverutils.serverlib.lib.data.NBTDataStorage;

import java.util.function.Consumer;

public class ForgeTeamDataEvent extends ForgeTeamEvent
{
	private final Consumer<NBTDataStorage.Data> callback;

	public ForgeTeamDataEvent(ForgeTeam team, Consumer<NBTDataStorage.Data> c)
	{
		super(team);
		callback = c;
	}

	public void register(NBTDataStorage.Data data)
	{
		callback.accept(data);
	}
}