package serverutils.serverlib.events.universe;

import serverutils.serverlib.lib.data.Universe;

public class UniverseClosedEvent extends UniverseEvent {

	public UniverseClosedEvent(Universe universe) {
		super(universe);
	}
}
