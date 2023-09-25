package serverutils.old.net;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import latmod.lib.ByteCount;
import serverutils.lib.LMAccessToken;
import serverutils.lib.api.net.LMNetworkWrapper;
import serverutils.old.world.LMPlayerServer;
import serverutils.old.world.LMWorldServer;
import serverutils.old.world.claims.ClaimedChunk;

public class MessageClaimChunk extends MessageServerUtilities {

    public static final int ID_CLAIM = 0;
    public static final int ID_UNCLAIM = 1;
    public static final int ID_UNCLAIM_ALL = 2;
    public static final int ID_UNCLAIM_ALL_DIMS = 3;
    public static final int ID_CLAIM_AND_LOAD = 4;
    public static final int ID_UNLOAD = 5;

    public MessageClaimChunk() {
        super(ByteCount.BYTE);
    }

    public MessageClaimChunk(int d, long t, int x, int z, int c) {
        this();
        io.writeByte(c);
        io.writeLong(t);
        io.writeInt(d);
        io.writeInt(x);
        io.writeInt(z);
    }

    public LMNetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.NET_WORLD;
    }

    public IMessage onMessage(MessageContext ctx) {
        int type = io.readUnsignedByte();
        long token = io.readLong();
        int dim = io.readInt();
        int cx = io.readInt();
        int cz = io.readInt();

        LMPlayerServer p = LMWorldServer.inst.getPlayer(ctx.getServerHandler().playerEntity);
        if (type == ID_CLAIM) {
            p.claimChunk(dim, cx, cz);
            return new MessageAreaUpdate(p, cx, cz, dim, 1, 1);
        } else if (type == ID_UNCLAIM) {
            if (token != 0L && LMAccessToken.equals(p.getPlayer(), token, false)) {
                ClaimedChunk c = LMWorldServer.inst.claimedChunks.getChunk(dim, cx, cz);
                if (c != null) {
                    LMPlayerServer p1 = LMWorldServer.inst.getPlayer(c.ownerID);
                    p1.unclaimChunk(dim, cx, cz);
                }
            } else p.unclaimChunk(dim, cx, cz);
            return new MessageAreaUpdate(p, cx, cz, dim, 1, 1);
        } else if (type == ID_UNCLAIM_ALL) {
            p.unclaimAllChunks(Integer.valueOf(dim));

        } else if (type == ID_UNCLAIM_ALL_DIMS) {
            p.unclaimAllChunks(null);
        } else if (type == ID_CLAIM_AND_LOAD) {
            p.claimChunk(dim, cx, cz);
            p.setLoaded(dim, cx, cz, true);
            return new MessageAreaUpdate(p, cx, cz, dim, 1, 1);
        } else if (type == ID_UNLOAD) {
            p.setLoaded(dim, cx, cz, false);
        }
        return null;
    }
}
