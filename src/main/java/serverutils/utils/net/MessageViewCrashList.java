package serverutils.utils.net;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import serverutils.lib.lib.io.DataIn;
import serverutils.lib.lib.io.DataOut;
import serverutils.lib.lib.net.MessageToClient;
import serverutils.lib.lib.net.NetworkWrapper;
import serverutils.utils.gui.GuiViewCrashList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

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
