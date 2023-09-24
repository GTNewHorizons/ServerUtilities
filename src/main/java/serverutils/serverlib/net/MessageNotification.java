package serverutils.serverlib.net;

import net.minecraft.util.IChatComponent;

import serverutils.serverlib.client.ServerLibClientEventHandler;
import serverutils.serverlib.lib.io.DataIn;
import serverutils.serverlib.lib.io.DataOut;
import serverutils.serverlib.lib.net.MessageToClient;
import serverutils.serverlib.lib.net.NetworkWrapper;
import serverutils.serverlib.lib.util.text_components.Notification;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class MessageNotification extends MessageToClient {

    private Notification notification;

    public MessageNotification() {}

    public MessageNotification(Notification notification) {
        this.notification = notification;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerLibNetHandler.GENERAL;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeTextComponent(notification);
    }

    @Override
    public void readData(DataIn data) {
        IChatComponent component = data.readTextComponent();
        if (component instanceof Notification) {
            notification = (Notification) component;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        ServerLibClientEventHandler.INST.onNotify(notification);
    }
}
