package serverutils.events.client;

import cpw.mods.fml.common.eventhandler.Cancelable;
import serverutils.events.ServerUtilitiesEvent;

@Cancelable
public class GuideEvent extends ServerUtilitiesEvent {

    private final String path;

    private GuideEvent(String p) {
        path = p;
    }

    public static class Check extends GuideEvent {

        private Check(String path) {
            super(path);
        }
    }

    public static class Open extends GuideEvent {

        private Open(String path) {
            super(path);
        }
    }

    public String getPath() {
        return path;
    }
}
