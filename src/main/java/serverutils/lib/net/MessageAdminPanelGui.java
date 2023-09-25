package serverutils.lib.net;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import serverutils.lib.lib.data.Action;
import serverutils.lib.lib.data.ForgePlayer;
import serverutils.lib.lib.data.Universe;
import serverutils.lib.lib.net.MessageToServer;
import serverutils.lib.lib.net.NetworkWrapper;
import serverutils.lib.ServerUtilitiesLibCommon;

public class MessageAdminPanelGui extends MessageToServer {

	@Override
	public NetworkWrapper getWrapper() {
		return ServerLibNetHandler.GENERAL;
	}

	@Override
	public void onMessage(EntityPlayerMP player) {
		List<Action.Inst> actions = new ArrayList<>();
		ForgePlayer p = Universe.get().getPlayer(player);
		NBTTagCompound data = new NBTTagCompound();

		for (Action a : ServerUtilitiesLibCommon.ADMIN_PANEL_ACTIONS.values()) {
			Action.Type type = a.getType(p, data);

			if (type.isVisible()) {
				actions.add(new Action.Inst(a, type));
			}
		}

		new MessageAdminPanelGuiResponse(actions).sendTo(player);
	}
}
