package serverutils.lib.api.tile;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public interface IGuiTile {

    Container getContainer(EntityPlayer ep, NBTTagCompound data);

    @SideOnly(Side.CLIENT)
    GuiScreen getGui(EntityPlayer ep, NBTTagCompound data);
}
