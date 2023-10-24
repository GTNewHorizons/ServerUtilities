package serverutils.events.player;

import java.util.function.Consumer;

import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.NBTDataStorage;

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
