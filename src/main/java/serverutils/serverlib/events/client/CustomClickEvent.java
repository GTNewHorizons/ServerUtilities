package serverutils.serverlib.events.client;

import serverutils.serverlib.events.ServerLibEvent;
import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.common.eventhandler.Cancelable;

@Cancelable
public class CustomClickEvent extends ServerLibEvent {
	private final ResourceLocation id;

	public CustomClickEvent(ResourceLocation _id)
	{
		id = _id;
	}

	public ResourceLocation getID()
	{
		return id;
	}
}