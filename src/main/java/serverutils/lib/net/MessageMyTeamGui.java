package serverutils.lib.net;

import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.lib.lib.data.ForgePlayer;
import serverutils.lib.lib.data.ServerUtilitiesLibAPI;
import serverutils.lib.lib.data.Universe;
import serverutils.lib.lib.net.MessageToServer;
import serverutils.lib.lib.net.NetworkWrapper;
import serverutils.lib.ServerUtilitiesLibGameRules;

public class MessageMyTeamGui extends MessageToServer {

	@Override
	public NetworkWrapper getWrapper() {
		return ServerLibNetHandler.MY_TEAM;
	}

	@Override
	public void onMessage(EntityPlayerMP player) {
		if (!ServerUtilitiesLibGameRules.canCreateTeam(player.worldObj) && !ServerUtilitiesLibGameRules.canJoinTeam(player.worldObj)) {
			ServerUtilitiesLibAPI.sendCloseGuiPacket(player);
		} else {
			ForgePlayer p = Universe.get().getPlayer(player);
			(p.hasTeam() ? new MessageMyTeamGuiResponse(p)
					: new MessageSelectTeamGui(p, ServerUtilitiesLibGameRules.canCreateTeam(player.worldObj))).sendTo(player);
		}
	}
}