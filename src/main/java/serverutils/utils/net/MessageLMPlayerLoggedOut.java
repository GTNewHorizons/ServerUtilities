package serverutils.utils.net;

import cpw.mods.fml.common.network.simpleimpl.*;
import cpw.mods.fml.relauncher.*;
import latmod.lib.ByteCount;
import serverutils.utils.api.EventLMPlayerClient;
import serverutils.utils.world.*;

public class MessageLMPlayerLoggedOut extends MessageServerUtilities {

    public MessageLMPlayerLoggedOut() {
        super(ByteCount.BYTE);
    }

    public MessageLMPlayerLoggedOut(LMPlayer p) {
        this();
        io.writeInt(p.getPlayerID());
    }

    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MessageLMPlayerLoggedOut m, MessageContext ctx) {
        LMPlayerClient p = LMWorldClient.inst.getPlayer(io.readInt());
        new EventLMPlayerClient.LoggedOut(p).post();
        p.isOnline = false;
        return null;
    }
}
