package serverutils.utils.net;

import cpw.mods.fml.common.network.simpleimpl.*;
import cpw.mods.fml.relauncher.*;
import latmod.lib.ByteCount;
import serverutils.utils.world.*;

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
