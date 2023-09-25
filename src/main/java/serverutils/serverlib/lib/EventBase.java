package serverutils.serverlib.lib;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.eventhandler.Event;

import serverutils.serverlib.ServerLib;
import serverutils.serverlib.ServerLibConfig;

public class EventBase extends Event {

	private boolean canPost = true;

	public boolean post() {
		if (canPost) {
			canPost = false;
			boolean b = MinecraftForge.EVENT_BUS.post(this);

			if (ServerLibConfig.debugging.log_events) {
				ServerLib.LOGGER.info("Event " + getClass().getName() + " fired, cancelled: " + b);
			}

			return b;
		}

		return false;
	}
}