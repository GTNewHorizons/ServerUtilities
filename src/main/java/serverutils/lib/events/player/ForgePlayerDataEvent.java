package serverutils.lib.events.player;

import serverutils.lib.lib.data.ForgePlayer;
import serverutils.lib.lib.data.NBTDataStorage;

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