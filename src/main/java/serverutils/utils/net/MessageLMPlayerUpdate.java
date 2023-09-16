package serverutils.utils.net;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import latmod.lib.ByteCount;
import serverutils.lib.api.client.ServerUtilitiesLibraryClient;
import serverutils.utils.api.EventLMPlayerClient;
import serverutils.utils.world.LMPlayerClient;
import serverutils.utils.world.LMPlayerServer;
import serverutils.utils.world.LMWorldClient;

public class MessageLMPlayerUpdate extends MessageServerUtilities {

    public MessageLMPlayerUpdate() {
        super(ByteCount.INT);
    }

    public MessageLMPlayerUpdate(LMPlayerServer p, boolean self) {
        this();
        io.writeInt(p.getPlayerID());
        io.writeBoolean(self);
        p.writeToNet(io, self);
    }

    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MessageContext ctx) {
        LMPlayerClient p = LMWorldClient.inst.getPlayer(io.readInt());
        boolean self = io.readBoolean();
        p.readFromNet(io, self);
        new EventLMPlayerClient.DataChanged(p).post();
        ServerUtilitiesLibraryClient.onGuiClientAction();
        return null;
    }
}
