package serverutils.events.client;

import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.common.eventhandler.Cancelable;
import serverutils.events.ServerUtilitiesEvent;

@Cancelable
public class CustomClickEvent extends ServerUtilitiesEvent {

    private final ResourceLocation id;

    public CustomClickEvent(ResourceLocation _id) {
        id = _id;
    }

    public ResourceLocation getID() {
        return id;
    }
}
