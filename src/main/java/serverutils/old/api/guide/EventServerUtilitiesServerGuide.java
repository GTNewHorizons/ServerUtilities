package serverutils.old.api.guide;

import serverutils.lib.api.EventLM;
import serverutils.old.world.LMPlayerServer;

public class EventServerUtilitiesServerGuide extends EventLM {

    public final ServerGuideFile file;
    public final LMPlayerServer player;
    public final boolean isOP;

    public EventServerUtilitiesServerGuide(ServerGuideFile f, LMPlayerServer p) {
        file = f;
        player = p;
        isOP = p.isOP();
    }
}
