package serverutils.serverlib.events.player;

import serverutils.serverlib.lib.data.ForgePlayer;
import serverutils.serverlib.lib.data.NBTDataStorage;

import java.util.function.Consumer;

public class ForgePlayerDataEvent extends ForgePlayerEvent {

	private final Consumer<NBTDataStorage.Data> callback;

	public ForgePlayerDataEvent(ForgePlayer player, Consumer<NBTDataStorage.Data> c) {
		super(player);
		callback = c;
	}

	public void register(NBTDataStorage.Data data) {
		callback.accept(data);
	}
}