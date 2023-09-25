package serverutils.lib.net;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import serverutils.lib.lib.io.DataIn;
import serverutils.lib.lib.io.DataOut;
import serverutils.lib.lib.net.MessageToServer;
import serverutils.lib.lib.net.NetworkWrapper;
import serverutils.lib.ServerUtilitiesLibCommon;

/**
 * @author LatvianModder
 */
public class MessageEditConfigResponse extends MessageToServer {

	private NBTTagCompound nbt;

	public MessageEditConfigResponse() {}

	public MessageEditConfigResponse(NBTTagCompound n) {
		nbt = n;
	}

	@Override
	public NetworkWrapper getWrapper() {
		return ServerLibNetHandler.EDIT_CONFIG;
	}

	@Override
	public void writeData(DataOut data) {
		data.writeNBT(nbt);
	}

	@Override
	public void readData(DataIn data) {
		nbt = data.readNBT();
	}

	@Override
	public void onMessage(EntityPlayerMP player) {
		ServerUtilitiesLibCommon.EditingConfig c = ServerUtilitiesLibCommon.TEMP_SERVER_CONFIG.get(player.getGameProfile().getId());
		// TODO: Logger

		if (c != null) {
			c.group.deserializeNBT(nbt);
			c.callback.onConfigSaved(c.group, player);
			ServerUtilitiesLibCommon.TEMP_SERVER_CONFIG.remove(player.getUniqueID());
		}
	}
}