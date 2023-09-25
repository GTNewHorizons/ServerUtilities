package serverutils.lib.events;

import java.util.function.Consumer;

public class SyncGamerulesEvent extends ServerUtilitiesLibEvent {

    private final Consumer<String> callback;

    public SyncGamerulesEvent(Consumer<String> c) {
        callback = c;
    }

    public void sync(String gamerule) {
        callback.accept(gamerule);
    }
}
