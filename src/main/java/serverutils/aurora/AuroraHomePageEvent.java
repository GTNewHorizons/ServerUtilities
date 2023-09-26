package serverutils.aurora;

import cpw.mods.fml.common.eventhandler.Event;
import serverutils.aurora.page.HomePageEntry;

public class AuroraHomePageEvent extends Event {

    private final AuroraServer server;
    private HomePageEntry entry;

    public AuroraHomePageEvent(AuroraServer s, HomePageEntry e) {
        server = s;
        entry = e;
    }

    public AuroraServer getAuroraServer() {
        return server;
    }

    public void add(HomePageEntry e) {
        entry.add(e);
    }
}
