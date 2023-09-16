package serverutils.utils.net;

import java.util.UUID;

import cpw.mods.fml.common.network.simpleimpl.*;
import latmod.lib.ByteCount;
import serverutils.lib.api.net.LMNetworkWrapper;
import serverutils.utils.badges.*;

public class MessageRequestBadge extends MessageServerUtilities {

    public MessageRequestBadge() {
        super(ByteCount.BYTE);
    }

    public MessageRequestBadge(UUID id) {
        this();
        io.writeUUID(id);
    }

    public LMNetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.NET_INFO;
    }

    public IMessage onMessage(MessageContext ctx) {
        UUID id = io.readUUID();
        Badge b = ServerBadges.getServerBadge(id);
        return (b == null || b == Badge.emptyBadge) ? null : new MessageSendBadge(id, b.getID());
    }
}
