package serverutils.old.api;

import serverutils.lib.api.EventLM;
import serverutils.old.world.LMWorldClient;

public class EventLMWorldClient extends EventLM {

    public final LMWorldClient world;
    public final boolean closed;

    public EventLMWorldClient(LMWorldClient w, boolean c) {
        world = w;
        closed = c;
    }
}
