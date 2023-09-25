package serverutils.old.net;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import latmod.lib.ByteCount;
import serverutils.old.world.LMPlayerServer;
import serverutils.old.world.LMWorldServer;

public class MessageClientAction extends MessageServerUtilities {

    public MessageClientAction() {
        super(ByteCount.BYTE);
    }

    MessageClientAction(ClientAction a, int e) {
        this();
        io.writeByte((a == null) ? ClientAction.NULL.getID() : a.getID());
        io.writeInt(e);
    }

    public IMessage onMessage(MessageContext ctx) {
        ClientAction action = ClientAction.get(io.readByte());
        int extra = io.readInt();
        LMPlayerServer owner = LMWorldServer.inst.getPlayer(ctx.getServerHandler().playerEntity);
        if (action.onAction(extra, owner)) owner.sendUpdate();
        return null;
    }
}
