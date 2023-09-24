package serverutils.serverlib.net;

import serverutils.serverlib.lib.gui.IGuiWrapper;
import serverutils.serverlib.lib.net.MessageToClient;
import serverutils.serverlib.lib.net.NetworkWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author LatvianModder
 */
public class MessageCloseGui extends MessageToClient
{
	@Override
	public NetworkWrapper getWrapper()
	{
		return ServerLibNetHandler.GENERAL;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void onMessage()
	{
		Minecraft mc = Minecraft.getMinecraft();

		if (mc.currentScreen instanceof IGuiWrapper)
		{
			((IGuiWrapper) mc.currentScreen).getGui().closeGui();
		}
		else if (!(mc.currentScreen instanceof GuiChat))
		{
			mc.thePlayer.closeScreen();
		}
	}
}