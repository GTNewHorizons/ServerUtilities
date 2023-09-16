package serverutils.lib.mod.client;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.api.gui.ContainerEmpty;
import serverutils.lib.api.gui.LMGuiHandler;
import serverutils.lib.api.item.ItemDisplay;
import serverutils.lib.mod.client.gui.GuiDisplayItem;

public class ServerUtilitiesLibraryGuiHandler extends LMGuiHandler {

    public static final ServerUtilitiesLibraryGuiHandler instance = new ServerUtilitiesLibraryGuiHandler(
            "ServerUtilitiesLibrary");

    public static final int DISPLAY_ITEM = 1;
    public static final int SECURITY = 2;

    public ServerUtilitiesLibraryGuiHandler(String s) {
        super(s);
    }

    public Container getContainer(EntityPlayer ep, int id, NBTTagCompound data) {
        return new ContainerEmpty(ep, null);
    }

    @SideOnly(Side.CLIENT)
    public GuiScreen getGui(EntityPlayer ep, int id, NBTTagCompound data) {
        if (id == DISPLAY_ITEM) return new GuiDisplayItem(ItemDisplay.readFromNBT(data));
        return null;
    }
}
