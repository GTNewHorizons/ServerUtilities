package serverutils.events.universe;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.common.eventhandler.Cancelable;
import serverutils.lib.data.Universe;

@Cancelable
public class PersistentScheduledTaskEvent extends UniverseEvent {

    private final ResourceLocation id;
    private final NBTTagCompound data;

    public PersistentScheduledTaskEvent(Universe universe, ResourceLocation i, NBTTagCompound d) {
        super(universe);
        id = i;
        data = d;
    }

    public ResourceLocation getID() {
        return id;
    }

    public NBTTagCompound getData() {
        return data;
    }
}
