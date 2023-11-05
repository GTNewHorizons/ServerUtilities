package serverutils.events;

import java.util.function.Consumer;

public class SyncGamerulesEvent extends ServerUtilitiesEvent {

    private final Consumer<String> callback;

    public SyncGamerulesEvent(Consumer<String> c) {
        callback = c;
    }

    public void sync(String gamerule) {
        callback.accept(gamerule);
    }
}
