package ftb.lib.api.tile;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

public interface IClientActionTile {

    void onClientAction(EntityPlayerMP ep, String action, NBTTagCompound data);
}
