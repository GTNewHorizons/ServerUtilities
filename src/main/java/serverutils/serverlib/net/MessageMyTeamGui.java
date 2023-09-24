package serverutils.serverlib.net;

import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.serverlib.ServerLibGameRules;
import serverutils.serverlib.lib.data.ServerLibAPI;
import serverutils.serverlib.lib.data.ForgePlayer;
import serverutils.serverlib.lib.data.Universe;
import serverutils.serverlib.lib.net.MessageToServer;
import serverutils.serverlib.lib.net.NetworkWrapper;

public class MessageMyTeamGui extends MessageToServer {

	@Override
	public NetworkWrapper getWrapper() {
		return ServerLibNetHandler.MY_TEAM;
	}

	@Override
	public void onMessage(EntityPlayerMP player) {
		if (!ServerLibGameRules.canCreateTeam(player.worldObj) && !ServerLibGameRules.canJoinTeam(player.worldObj)) {
			ServerLibAPI.sendCloseGuiPacket(player);
		} else {
			ForgePlayer p = Universe.get().getPlayer(player);
			(p.hasTeam() ? new MessageMyTeamGuiResponse(p)
					: new MessageSelectTeamGui(p, ServerLibGameRules.canCreateTeam(player.worldObj))).sendTo(player);
		}
	}
}