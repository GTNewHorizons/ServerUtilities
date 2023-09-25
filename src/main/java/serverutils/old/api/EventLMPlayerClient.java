package serverutils.old.api;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import serverutils.old.world.*;

public abstract class EventLMPlayerClient extends EventLMPlayer {

    public final LMPlayerClient player;

    public EventLMPlayerClient(LMPlayerClient p) {
        player = p;
    }

    public LMPlayer getPlayer() {
        return player;
    }

    public Side getSide() {
        return Side.CLIENT;
    }

    // Events //

    public static class DataChanged extends EventLMPlayerClient {

        public DataChanged(LMPlayerClient p) {
            super(p);
        }
    }

    public static class LoggedIn extends EventLMPlayerClient {

        public final boolean firstTime;

        public LoggedIn(LMPlayerClient p, boolean b) {
            super(p);
            firstTime = b;
        }
    }

    public static class LoggedOut extends EventLMPlayerClient {

        public LoggedOut(LMPlayerClient p) {
            super(p);
        }
    }

    public static class DataLoaded extends EventLMPlayerClient {

        public DataLoaded(LMPlayerClient p) {
            super(p);
        }
    }

    public static class CustomInfo extends EventLMPlayerClient {

        public final List<String> info;

        public CustomInfo(LMPlayerClient p, List<String> l) {
            super(p);
            info = l;
        }
    }

    public static class PlayerDied extends EventLMPlayerClient {

        public PlayerDied(LMPlayerClient p) {
            super(p);
        }
    }
}
