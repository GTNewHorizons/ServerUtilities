package serverutils.net;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.gui.IGuiWrapper;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;

public class MessageCloseGui extends MessageToClient {

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.GENERAL;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.currentScreen instanceof IGuiWrapper guiWrapper) {
            guiWrapper.getGui().closeGui();
        } else if (!(mc.currentScreen instanceof GuiChat)) {
            mc.thePlayer.closeScreen();
        }
    }
}
