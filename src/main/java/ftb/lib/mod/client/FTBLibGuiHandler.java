package ftb.lib.mod.client;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ftb.lib.api.gui.ContainerEmpty;
import ftb.lib.api.gui.LMGuiHandler;
import ftb.lib.api.item.ItemDisplay;
import ftb.lib.mod.client.gui.GuiDisplayItem;

public class FTBLibGuiHandler extends LMGuiHandler {

    public static final FTBLibGuiHandler instance = new FTBLibGuiHandler("FTBL");

    public static final int DISPLAY_ITEM = 1;
    public static final int SECURITY = 2;

    public FTBLibGuiHandler(String s) {
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
