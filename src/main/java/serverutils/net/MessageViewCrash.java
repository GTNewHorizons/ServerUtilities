package serverutils.net;

import java.io.File;

import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.ServerUtilitiesConfig;
import serverutils.ServerUtilitiesPermissions;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.io.DataReader;
import serverutils.lib.net.MessageToServer;
import serverutils.lib.net.NetworkWrapper;
import serverutils.lib.util.permission.PermissionAPI;

public class MessageViewCrash extends MessageToServer {

    private String id;

    public MessageViewCrash() {}

    public MessageViewCrash(String s) {
        id = s;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.FILES;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeString(id);
    }

    @Override
    public void readData(DataIn data) {
        id = data.readString();
    }

    @Override
    public void onMessage(EntityPlayerMP player) {
        if (PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.CRASH_REPORTS_VIEW)) {
            try {
                File file = player.mcServer.getFile("crash-reports/crash-" + id + ".txt");

                if (file.exists()) {
                    new MessageViewCrashResponse(file.getName(), DataReader.get(file).stringList()).sendTo(player);
                }
            } catch (Exception ex) {
                if (ServerUtilitiesConfig.debugging.print_more_errors) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
