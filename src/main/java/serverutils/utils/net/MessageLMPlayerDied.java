package serverutils.utils.net;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import latmod.lib.ByteCount;
import serverutils.utils.api.EventLMPlayerClient;
import serverutils.utils.world.LMPlayer;
import serverutils.utils.world.LMPlayerClient;
import serverutils.utils.world.LMWorldClient;

public class MessageLMPlayerDied extends MessageServerUtilities {

    public MessageLMPlayerDied() {
        super(ByteCount.BYTE);
    }

    public MessageLMPlayerDied(LMPlayer p) {
        this();
        io.writeInt(p.getPlayerID());
    }

    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MessageContext ctx) {
        LMPlayerClient p = LMWorldClient.inst.getPlayer(io.readInt());
        if (p != null) new EventLMPlayerClient.PlayerDied(p).post();
        return null;
    }
}
