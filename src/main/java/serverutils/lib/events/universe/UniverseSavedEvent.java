package serverutils.lib.events.universe;

import net.minecraft.nbt.NBTTagCompound;

import serverutils.lib.lib.data.Universe;

public class UniverseSavedEvent extends UniverseEvent {

    private NBTTagCompound data;

    public UniverseSavedEvent(Universe universe, NBTTagCompound nbt) {
        super(universe);
        data = nbt;
    }

    public void setData(String id, NBTTagCompound nbt) {
        data.setTag(id, nbt);
    }
}
