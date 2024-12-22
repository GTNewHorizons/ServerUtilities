package serverutils.net;

import net.minecraft.util.IChatComponent;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.client.NotificationHandler;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;

public class MessageNotification extends MessageToClient {

    private IChatComponent notification;

    public MessageNotification() {}

    public MessageNotification(IChatComponent notification) {
        this.notification = notification;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.GENERAL;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeTextComponent(notification);
    }

    @Override
    public void readData(DataIn data) {
        notification = data.readTextComponent();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        NotificationHandler.onNotify(notification);
    }
}
