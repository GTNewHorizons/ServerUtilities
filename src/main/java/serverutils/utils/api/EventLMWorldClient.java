package serverutils.utils.api;

import serverutils.lib.api.EventLM;
import serverutils.utils.world.LMWorldClient;

public class EventLMWorldClient extends EventLM {

    public final LMWorldClient world;
    public final boolean closed;

    public EventLMWorldClient(LMWorldClient w, boolean c) {
        world = w;
        closed = c;
    }
}
