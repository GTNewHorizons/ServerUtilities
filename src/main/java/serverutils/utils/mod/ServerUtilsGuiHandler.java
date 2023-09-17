package serverutils.utils.mod;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.api.gui.ContainerEmpty;
import serverutils.lib.api.gui.LMGuiHandler;
import serverutils.utils.mod.client.gui.claims.GuiClaimChunks;
import serverutils.utils.mod.client.gui.friends.GuideFriendsGUI;
import serverutils.utils.mod.client.gui.guide.GuiGuide;

public class ServerUtilsGuiHandler extends LMGuiHandler {

    public static final ServerUtilsGuiHandler instance = new ServerUtilsGuiHandler(ServerUtilitiesFinals.MOD_ID);

    public static final int FRIENDS = 1;
    public static final int SECURITY = 2;
    public static final int ADMIN_CLAIMS = 3;

    public ServerUtilsGuiHandler(String s) {
        super(s);
    }

    public Container getContainer(EntityPlayer ep, int id, NBTTagCompound data) {
        return new ContainerEmpty(ep, null);
    }

    @SideOnly(Side.CLIENT)
    public GuiScreen getGui(EntityPlayer ep, int id, NBTTagCompound data) {
        if (id == FRIENDS) return new GuiGuide(null, new GuideFriendsGUI());
        else if (id == ADMIN_CLAIMS) return new GuiClaimChunks(data.getLong("T"));
        return null;
    }
}
