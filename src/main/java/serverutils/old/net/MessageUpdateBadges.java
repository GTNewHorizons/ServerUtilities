package serverutils.old.net;

import java.util.Collection;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import latmod.lib.ByteCount;
import serverutils.lib.api.net.LMNetworkWrapper;
import serverutils.old.badges.Badge;
import serverutils.old.badges.ClientBadges;

public class MessageUpdateBadges extends MessageServerUtilities {

    public MessageUpdateBadges() {
        super(ByteCount.INT);
    }

    public MessageUpdateBadges(Collection<Badge> badges) {
        this();
        io.writeInt(badges.size());

        if (!badges.isEmpty()) {
            for (Badge b : badges) {
                io.writeUTF(b.getID());
                io.writeUTF(b.imageURL);
            }
        }
    }

    public LMNetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.NET_INFO;
    }

    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MessageContext ctx) {
        ClientBadges.clear();
        int s = io.readInt();

        if (s > 0) {
            for (int i = 0; i < s; i++) {
                String id = io.readUTF();
                String url = io.readUTF();
                ClientBadges.addBadge(new Badge(id, url));
            }
        }

        return null;
    }
}
