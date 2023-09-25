package serverutils.utils.net;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;

import com.feed_the_beast.ftblib.lib.data.Universe;
import com.feed_the_beast.ftblib.lib.io.DataIn;
import com.feed_the_beast.ftblib.lib.io.DataOut;
import com.feed_the_beast.ftblib.lib.net.MessageToServer;
import com.feed_the_beast.ftblib.lib.net.NetworkWrapper;
import serverutils.utils.data.FTBUtilitiesUniverseData;

public class MessageRequestBadge extends MessageToServer {

    private UUID playerId;

    public MessageRequestBadge() {}

    public MessageRequestBadge(UUID player) {
        playerId = player;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return FTBUtilitiesNetHandler.GENERAL;
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
        String badge = FTBUtilitiesUniverseData.getBadge(Universe.get(), playerId);
        new MessageSendBadge(playerId, badge).sendTo(player);
    }
}
