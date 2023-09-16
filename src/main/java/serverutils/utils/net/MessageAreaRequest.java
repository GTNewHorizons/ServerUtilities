package serverutils.utils.net;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import latmod.lib.ByteCount;
import latmod.lib.MathHelperLM;
import serverutils.lib.api.net.LMNetworkWrapper;
import serverutils.utils.world.LMWorldServer;

public class MessageAreaRequest extends MessageServerUtilities {

    public MessageAreaRequest() {
        super(ByteCount.BYTE);
    }

    public MessageAreaRequest(int x, int y, int w, int h) {
        this();
        io.writeInt(x);
        io.writeInt(y);
        io.writeInt(MathHelperLM.clampInt(w, 1, 255));
        io.writeInt(MathHelperLM.clampInt(h, 1, 255));
    }

    public LMNetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.NET_WORLD;
    }

    public IMessage onMessage(MessageContext ctx) {
        int chunkX = io.readInt();
        int chunkY = io.readInt();
        int sizeX = io.readInt();
        int sizeY = io.readInt();

        EntityPlayerMP ep = ctx.getServerHandler().playerEntity;
        return new MessageAreaUpdate(LMWorldServer.inst.getPlayer(ep), chunkX, chunkY, ep.dimension, sizeX, sizeY);
    }
}
