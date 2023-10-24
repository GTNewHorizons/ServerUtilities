package serverutils.net;

import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.ServerUtilitiesGameRules;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.ServerUtilitiesAPI;
import serverutils.lib.data.Universe;
import serverutils.lib.net.MessageToServer;
import serverutils.lib.net.NetworkWrapper;

public class MessageMyTeamGui extends MessageToServer {

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.MY_TEAM;
    }

    @Override
    public void onMessage(EntityPlayerMP player) {
        if (!ServerUtilitiesGameRules.canCreateTeam(player.worldObj)
                && !ServerUtilitiesGameRules.canJoinTeam(player.worldObj)) {
            ServerUtilitiesAPI.sendCloseGuiPacket(player);
        } else {
            ForgePlayer p = Universe.get().getPlayer(player);
            (p.hasTeam() ? new MessageMyTeamGuiResponse(p)
                    : new MessageSelectTeamGui(p, ServerUtilitiesGameRules.canCreateTeam(player.worldObj)))
                            .sendTo(player);
        }
    }
}
