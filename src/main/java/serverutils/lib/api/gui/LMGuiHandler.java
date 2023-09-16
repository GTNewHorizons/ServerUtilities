package serverutils.lib.api.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.FakePlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.mod.ServerUtilitiesLibraryMod;
import serverutils.lib.mod.net.MessageOpenGui;

public abstract class LMGuiHandler {

    public final String ID;

    public LMGuiHandler(String s) {
        ID = s;
    }

    public abstract Container getContainer(EntityPlayer ep, int id, NBTTagCompound data);

    @SideOnly(Side.CLIENT)
    public abstract GuiScreen getGui(EntityPlayer ep, int id, NBTTagCompound data);

    public void openGui(EntityPlayer ep, int id, NBTTagCompound data) {
        if (ep instanceof FakePlayer) return;
        else if (ep instanceof EntityPlayerMP) {
            Container c = getContainer(ep, id, data);
            if (c == null) return;

            EntityPlayerMP epM = (EntityPlayerMP) ep;
            epM.getNextWindowId();
            epM.closeContainer();
            epM.openContainer = c;
            epM.openContainer.windowId = epM.currentWindowId;
            epM.openContainer.addCraftingToCrafters(epM);
            new MessageOpenGui(ID, id, data, epM.currentWindowId).sendTo(epM);
        } else if (!ServerUtilitiesLib.getEffectiveSide().isServer()) ServerUtilitiesLibraryMod.proxy
                .openClientGui((ep == null) ? ServerUtilitiesLibraryMod.proxy.getClientPlayer() : ep, ID, id, data);
    }
}
