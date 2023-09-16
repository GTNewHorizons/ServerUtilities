package ftb.lib.api.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public interface IClientActionItem {

    ItemStack onClientAction(ItemStack is, EntityPlayer ep, String action, NBTTagCompound data);
}
