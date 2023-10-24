package serverutils.net;

import java.io.File;

import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.ServerUtilitiesConfig;
import serverutils.ServerUtilitiesPermissions;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToServer;
import serverutils.lib.net.NetworkWrapper;
import serverutils.lib.util.permission.PermissionAPI;

public class MessageViewCrashDelete extends MessageToServer {

    private String id;

    public MessageViewCrashDelete() {}

    public MessageViewCrashDelete(String s) {
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
            File folder = player.mcServer.getFile("crash-reports");

            if (PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.CRASH_REPORTS_DELETE)) {
                try {
                    File file = new File(folder, id);

                    if (file.exists() && file.getParentFile().equals(folder)) {
                        file.delete();
                    }
                } catch (Exception ex) {
                    if (ServerUtilitiesConfig.debugging.print_more_errors) {
                        ex.printStackTrace();
                    }
                }
            }

            new MessageViewCrashList(folder).sendTo(player);
        }
    }
}
