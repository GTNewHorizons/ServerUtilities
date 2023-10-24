package serverutils.net;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.client.gui.GuiViewCrashList;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;

public class MessageViewCrashList extends MessageToClient {

    private Collection<String> list;

    public MessageViewCrashList() {}

    public MessageViewCrashList(File folder) {
        list = new ArrayList<>();

        File[] files = folder.listFiles();

        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().endsWith(".txt") && f.getName().startsWith("crash-")) {
                    list.add(f.getName().replace("crash-", "").replace(".txt", ""));
                }
            }
        }
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.FILES;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeCollection(list, DataOut.STRING);
    }

    @Override
    public void readData(DataIn data) {
        list = data.readCollection(DataIn.STRING);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        new GuiViewCrashList(list).openGui();
    }
}
