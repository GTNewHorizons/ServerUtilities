package serverutils.net;

import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.lib.net.MessageToServer;
import serverutils.lib.net.NetworkWrapper;

public class MessageCommandsRequest extends MessageToServer {

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.GENERAL;
    }

    @Override
    public void onMessage(EntityPlayerMP player) {
        new MessageCommandsResponse(player).sendTo(player);
    }
}
