package serverutils.lib.events.universe;

import serverutils.lib.events.ServerUtilitiesLibEvent;
import serverutils.lib.lib.data.Universe;

public abstract class UniverseEvent extends ServerUtilitiesLibEvent {

	private final Universe universe;

	public UniverseEvent(Universe u) {
		universe = u;
	}

	public Universe getUniverse() {
		return universe;
	}
}
