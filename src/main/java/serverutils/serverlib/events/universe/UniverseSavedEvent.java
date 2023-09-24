package serverutils.serverlib.events.universe;

import serverutils.serverlib.lib.data.Universe;
import net.minecraft.nbt.NBTTagCompound;

public class UniverseSavedEvent extends UniverseEvent
{
	private NBTTagCompound data;

	public UniverseSavedEvent(Universe universe, NBTTagCompound nbt)
	{
		super(universe);
		data = nbt;
	}

	public void setData(String id, NBTTagCompound nbt)
	{
		data.setTag(id, nbt);
	}
}