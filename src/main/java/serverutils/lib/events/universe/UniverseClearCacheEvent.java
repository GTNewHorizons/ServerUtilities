package serverutils.lib.events.universe;

import serverutils.lib.lib.data.Universe;

public class UniverseClearCacheEvent extends UniverseEvent {

    public UniverseClearCacheEvent(Universe universe) {
        super(universe);
    }
}
