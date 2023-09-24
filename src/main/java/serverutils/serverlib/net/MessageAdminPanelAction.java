package serverutils.serverlib.net;

import serverutils.serverlib.ServerLibCommon;
import serverutils.serverlib.lib.data.Action;
import serverutils.serverlib.lib.data.ForgePlayer;
import serverutils.serverlib.lib.data.Universe;
import serverutils.serverlib.lib.io.DataIn;
import serverutils.serverlib.lib.io.DataOut;
import serverutils.serverlib.lib.net.MessageToServer;
import serverutils.serverlib.lib.net.NetworkWrapper;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

/**
 * @author LatvianModder
 */
public class MessageAdminPanelAction extends MessageToServer
{
	private ResourceLocation action;

	public MessageAdminPanelAction()
	{
	}

	public MessageAdminPanelAction(ResourceLocation id)
	{
		action = id;
	}

	@Override
	public NetworkWrapper getWrapper()
	{
		return ServerLibNetHandler.GENERAL;
	}

	@Override
	public void writeData(DataOut data)
	{
		data.writeResourceLocation(action);
	}

	@Override
	public void readData(DataIn data)
	{
		action = data.readResourceLocation();
	}

	@Override
	public void onMessage(EntityPlayerMP player)
	{
		Action a = ServerLibCommon.ADMIN_PANEL_ACTIONS.get(action);

		if (a != null)
		{
			ForgePlayer p = Universe.get().getPlayer(player);
			NBTTagCompound data = new NBTTagCompound();

			if (a.getType(p, data).isEnabled())
			{
				a.onAction(p, data);
			}
		}
	}
}