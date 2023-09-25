package serverutils.old.api;

import latmod.lib.util.Phase;
import serverutils.lib.api.EventLM;
import serverutils.old.world.LMWorldServer;

public class EventLMWorldServer extends EventLM {

    public final LMWorldServer world;

    public EventLMWorldServer(LMWorldServer w) {
        world = w;
    }

    public static class Loaded extends EventLMWorldServer {

        public final Phase phase;

        public Loaded(LMWorldServer w, Phase p) {
            super(w);
            phase = p;
        }
    }

    public static class Saved extends EventLMWorldServer {

        public Saved(LMWorldServer w) {
            super(w);
        }
    }
}
