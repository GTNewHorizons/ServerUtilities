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
public class MessageMyTeamAction extends MessageToServer
{
	private ResourceLocation action;
	private NBTTagCompound nbt;

	public MessageMyTeamAction()
	{
	}

	public MessageMyTeamAction(ResourceLocation id, NBTTagCompound data)
	{
		action = id;
		nbt = data;
	}

	@Override
	public NetworkWrapper getWrapper()
	{
		return ServerLibNetHandler.MY_TEAM;
	}

	@Override
	public void writeData(DataOut data)
	{
		data.writeResourceLocation(action);
		data.writeNBT(nbt);
	}

	@Override
	public void readData(DataIn data)
	{
		action = data.readResourceLocation();
		nbt = data.readNBT();
	}

	@Override
	public void onMessage(EntityPlayerMP player)
	{
		Action a = ServerLibCommon.TEAM_GUI_ACTIONS.get(action);

		if (a != null)
		{
			ForgePlayer p = Universe.get().getPlayer(player);

			if (p.hasTeam() && a.getType(p, nbt).isEnabled())
			{
				a.onAction(p, nbt);
			}
		}
	}
}