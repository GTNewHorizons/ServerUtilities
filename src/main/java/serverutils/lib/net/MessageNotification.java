package serverutils.lib.net;

import net.minecraft.util.IChatComponent;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.lib.io.DataIn;
import serverutils.lib.lib.io.DataOut;
import serverutils.lib.lib.net.MessageToClient;
import serverutils.lib.lib.net.NetworkWrapper;
import serverutils.lib.lib.util.text_components.Notification;
import serverutils.mod.handlers.ServerUtilitiesClientEventHandler;

public class MessageNotification extends MessageToClient {

    private IChatComponent notification;

    public MessageNotification() {}

    public MessageNotification(IChatComponent notification) {
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
        notification = data.readTextComponent();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        ServerUtilitiesClientEventHandler.INST.onNotify(notification);
    }
}
