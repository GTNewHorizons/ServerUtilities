package serverutils.utils.net;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.lib.lib.data.Universe;
import serverutils.lib.lib.io.DataIn;
import serverutils.lib.lib.io.DataOut;
import serverutils.lib.lib.net.MessageToServer;
import serverutils.lib.lib.net.NetworkWrapper;
import serverutils.utils.data.ServerUtilitiesUniverseData;

public class MessageRequestBadge extends MessageToServer {

    private UUID playerId;

    public MessageRequestBadge() {}

    public MessageRequestBadge(UUID player) {
        playerId = player;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.GENERAL;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeUUID(playerId);
    }

    @Override
    public void readData(DataIn data) {
        playerId = data.readUUID();
    }

    @Override
    public void onMessage(EntityPlayerMP player) {
        String badge = ServerUtilitiesUniverseData.getBadge(Universe.get(), playerId);
        new MessageSendBadge(playerId, badge).sendTo(player);
    }
}
