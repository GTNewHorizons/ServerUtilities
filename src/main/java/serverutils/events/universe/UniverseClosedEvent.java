package serverutils.events.universe;

import serverutils.lib.data.Universe;

public class UniverseClosedEvent extends UniverseEvent {

    public UniverseClosedEvent(Universe universe) {
        super(universe);
    }
}
