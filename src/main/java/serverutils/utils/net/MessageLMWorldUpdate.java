package serverutils.utils.net;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import latmod.lib.ByteCount;
import serverutils.utils.world.LMPlayerServer;
import serverutils.utils.world.LMWorldClient;
import serverutils.utils.world.LMWorldServer;

public class MessageLMWorldUpdate extends MessageServerUtilities {

    public MessageLMWorldUpdate() {
        super(ByteCount.INT);
    }

    public MessageLMWorldUpdate(LMWorldServer w, LMPlayerServer self) {
        this();
        w.writeDataToNet(io, self, false);
    }

    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MessageContext ctx) {
        LMWorldClient.inst.readDataFromNet(io, false);
        return null;
    }
}
