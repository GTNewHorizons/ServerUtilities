package serverutils.lib.lib;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.eventhandler.Event;
import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.ServerUtilitiesLibConfig;

public class EventBase extends Event {

    private boolean canPost = true;

    public boolean post() {
        if (canPost) {
            canPost = false;
            boolean b = MinecraftForge.EVENT_BUS.post(this);

            if (ServerUtilitiesLibConfig.debugging.log_events) {
                ServerUtilitiesLib.LOGGER.info("Event " + getClass().getName() + " fired, cancelled: " + b);
            }

            return b;
        }

        return false;
    }
}
