package serverutils.lib.events.universe;

import serverutils.lib.lib.data.Universe;

public class UniverseClosedEvent extends UniverseEvent {

	public UniverseClosedEvent(Universe universe) {
		super(universe);
	}
}
