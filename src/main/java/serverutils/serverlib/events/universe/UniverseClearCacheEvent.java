package serverutils.serverlib.events.universe;

import serverutils.serverlib.lib.data.Universe;

public class UniverseClearCacheEvent extends UniverseEvent {

	public UniverseClearCacheEvent(Universe universe) {
		super(universe);
	}
}
