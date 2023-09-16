package ftb.lib.api;

import ftb.lib.FTBWorld;

public class EventFTBWorldClient extends EventLM {

    public final FTBWorld world;

    public EventFTBWorldClient(FTBWorld w) {
        world = w;
    }
}
