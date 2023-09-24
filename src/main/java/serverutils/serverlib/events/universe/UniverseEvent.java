package serverutils.serverlib.events.universe;

import serverutils.serverlib.events.ServerLibEvent;
import serverutils.serverlib.lib.data.Universe;

public abstract class UniverseEvent extends ServerLibEvent {

	private final Universe universe;

	public UniverseEvent(Universe u) {
		universe = u;
	}

	public Universe getUniverse() {
		return universe;
	}
}
