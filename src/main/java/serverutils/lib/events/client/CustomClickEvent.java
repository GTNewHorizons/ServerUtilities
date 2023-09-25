package serverutils.lib.events.client;

import serverutils.lib.events.ServerUtilitiesLibEvent;
import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.common.eventhandler.Cancelable;

@Cancelable
public class CustomClickEvent extends ServerUtilitiesLibEvent {

	private final ResourceLocation id;

	public CustomClickEvent(ResourceLocation _id) {
		id = _id;
	}

	public ResourceLocation getID() {
		return id;
	}
}