package serverutils.lib;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.eventhandler.Event;
import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;

public class EventBase extends Event {

    private boolean canPost = true;

    public boolean post() {
        if (canPost) {
            canPost = false;
            boolean b = MinecraftForge.EVENT_BUS.post(this);

            if (ServerUtilitiesConfig.debugging.log_events) {
                ServerUtilities.LOGGER.info("Event " + getClass().getName() + " fired, cancelled: " + b);
            }

            return b;
        }

        return false;
    }
}
