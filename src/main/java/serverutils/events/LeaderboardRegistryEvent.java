package serverutils.events;

import java.util.function.Consumer;

import serverutils.data.Leaderboard;

public class LeaderboardRegistryEvent extends ServerUtilitiesEvent {

    private final Consumer<Leaderboard> callback;

    public LeaderboardRegistryEvent(Consumer<Leaderboard> c) {
        callback = c;
    }

    public void register(Leaderboard entry) {
        callback.accept(entry);
    }
}
