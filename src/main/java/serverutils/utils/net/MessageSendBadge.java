package serverutils.utils.net;

import java.util.UUID;

import cpw.mods.fml.common.network.simpleimpl.*;
import cpw.mods.fml.relauncher.*;
import latmod.lib.ByteCount;
import serverutils.lib.api.net.LMNetworkWrapper;
import serverutils.utils.badges.ClientBadges;

public class MessageSendBadge extends MessageServerUtilities {

    public MessageSendBadge() {
        super(ByteCount.BYTE);
    }

    public MessageSendBadge(UUID player, String id) {
        this();
        io.writeUUID(player);
        io.writeUTF(id);
    }

    public LMNetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.NET_INFO;
    }

    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MessageContext ctx) {
        UUID player = io.readUUID();
        String badge = io.readUTF();
        ClientBadges.setClientBadge(player, badge);
        return null;
    }
}
