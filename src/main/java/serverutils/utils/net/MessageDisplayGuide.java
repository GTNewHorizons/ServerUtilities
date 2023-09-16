package serverutils.utils.net;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import latmod.lib.ByteCount;
import latmod.lib.json.JsonElementIO;
import serverutils.lib.api.client.ServerUtilitiesLibraryClient;
import serverutils.lib.api.net.LMNetworkWrapper;
import serverutils.utils.api.guide.GuidePage;
import serverutils.utils.mod.client.gui.guide.GuiGuide;

public class MessageDisplayGuide extends MessageServerUtilities {

    public MessageDisplayGuide() {
        super(ByteCount.INT);
    }

    public MessageDisplayGuide(GuidePage file) {
        this();
        file.cleanup();
        io.writeUTF(file.getID());
        JsonElementIO.write(io, file.getSerializableElement());
    }

    public LMNetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.NET_INFO;
    }

    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MessageContext ctx) {
        GuidePage file = new GuidePage(io.readUTF());
        file.func_152753_a(JsonElementIO.read(io));
        ServerUtilitiesLibraryClient.openGui(new GuiGuide(null, file));
        return null;
    }
}
