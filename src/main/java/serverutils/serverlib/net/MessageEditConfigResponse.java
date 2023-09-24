package serverutils.serverlib.net;

import serverutils.serverlib.ServerLibCommon;
import serverutils.serverlib.lib.io.DataIn;
import serverutils.serverlib.lib.io.DataOut;
import serverutils.serverlib.lib.net.MessageToServer;
import serverutils.serverlib.lib.net.NetworkWrapper;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

/**
 * @author LatvianModder
 */
public class MessageEditConfigResponse extends MessageToServer
{
	private NBTTagCompound nbt;

	public MessageEditConfigResponse()
	{
	}

	public MessageEditConfigResponse(NBTTagCompound n)
	{
		nbt = n;
	}

	@Override
	public NetworkWrapper getWrapper()
	{
		return ServerLibNetHandler.EDIT_CONFIG;
	}

	@Override
	public void writeData(DataOut data)
	{
		data.writeNBT(nbt);
	}

	@Override
	public void readData(DataIn data)
	{
		nbt = data.readNBT();
	}

	@Override
	public void onMessage(EntityPlayerMP player)
	{
		ServerLibCommon.EditingConfig c = ServerLibCommon.TEMP_SERVER_CONFIG.get(player.getGameProfile().getId());
		//TODO: Logger

		if (c != null)
		{
			c.group.deserializeNBT(nbt);
			c.callback.onConfigSaved(c.group, player);
			ServerLibCommon.TEMP_SERVER_CONFIG.remove(player.getUniqueID());
		}
	}
}