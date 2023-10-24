package serverutils.net;

import java.util.Collection;
import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.client.gui.GuiViewCrash;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;

public class MessageViewCrashResponse extends MessageToClient {

    private String name;
    private Collection<String> text;

    public MessageViewCrashResponse() {}

    public MessageViewCrashResponse(String n, List<String> l) {
        name = n;
        text = l;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.FILES;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeString(name);
        data.writeCollection(text, DataOut.STRING);
    }

    @Override
    public void readData(DataIn data) {
        name = data.readString();
        text = data.readCollection(DataIn.STRING);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        new GuiViewCrash(name, text).openGui();
    }
}
