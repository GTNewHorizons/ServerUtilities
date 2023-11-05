package serverutils.events.universe;

import serverutils.lib.data.Universe;

public class UniverseClearCacheEvent extends UniverseEvent {

    public UniverseClearCacheEvent(Universe universe) {
        super(universe);
    }
}
