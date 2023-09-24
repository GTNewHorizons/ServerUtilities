package serverutils.serverlib.net;

import serverutils.serverlib.ServerLibCommon;
import serverutils.serverlib.lib.data.Action;
import serverutils.serverlib.lib.data.ForgePlayer;
import serverutils.serverlib.lib.data.Universe;
import serverutils.serverlib.lib.net.MessageToServer;
import serverutils.serverlib.lib.net.NetworkWrapper;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;

public class MessageAdminPanelGui extends MessageToServer
{
	@Override
	public NetworkWrapper getWrapper()
	{
		return ServerLibNetHandler.GENERAL;
	}

	@Override
	public void onMessage(EntityPlayerMP player)
	{
		List<Action.Inst> actions = new ArrayList<>();
		ForgePlayer p = Universe.get().getPlayer(player);
		NBTTagCompound data = new NBTTagCompound();

		for (Action a : ServerLibCommon.ADMIN_PANEL_ACTIONS.values())
		{
			Action.Type type = a.getType(p, data);

			if (type.isVisible())
			{
				actions.add(new Action.Inst(a, type));
			}
		}

		new MessageAdminPanelGuiResponse(actions).sendTo(player);
	}
}