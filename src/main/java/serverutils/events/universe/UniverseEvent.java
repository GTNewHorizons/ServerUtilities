package serverutils.events.universe;

import serverutils.events.ServerUtilitiesEvent;
import serverutils.lib.data.Universe;

public abstract class UniverseEvent extends ServerUtilitiesEvent {

    private final Universe universe;

    public UniverseEvent(Universe u) {
        universe = u;
    }

    public Universe getUniverse() {
        return universe;
    }
}
