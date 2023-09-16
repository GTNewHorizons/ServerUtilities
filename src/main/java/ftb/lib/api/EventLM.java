package ftb.lib.api;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.eventhandler.Event;

public abstract class EventLM extends Event {

    public final void post() {
        MinecraftForge.EVENT_BUS.post(this);
    }
}
