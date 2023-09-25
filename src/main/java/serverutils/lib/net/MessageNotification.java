package serverutils.lib.net;

import net.minecraft.util.IChatComponent;

import serverutils.lib.lib.io.DataIn;
import serverutils.lib.lib.io.DataOut;
import serverutils.lib.lib.net.MessageToClient;
import serverutils.lib.lib.net.NetworkWrapper;
import serverutils.lib.client.ServerUtilitiesLibClientEventHandler;
import serverutils.lib.lib.util.text_components.Notification;

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
        ServerUtilitiesLibClientEventHandler.INST.onNotify(notification);
    }
}
