package serverutils.lib.mod.net;

import cpw.mods.fml.common.network.simpleimpl.*;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import latmod.lib.ByteCount;
import latmod.lib.json.JsonElementIO;
import serverutils.lib.EnumScreen;
import serverutils.lib.api.client.ServerUtilitiesLibraryClient;
import serverutils.lib.api.net.LMNetworkWrapper;
import serverutils.lib.api.net.MessageLM;
import serverutils.lib.api.notification.ClientNotifications;
import serverutils.lib.api.notification.Notification;
import serverutils.lib.mod.client.ServerUtilitiesLibraryModClient;

public class MessageNotifyPlayer extends MessageLM {

    public MessageNotifyPlayer() {
        super(ByteCount.SHORT);
    }

    public MessageNotifyPlayer(Notification n) {
        this();
        JsonElementIO.write(io, n.getSerializableElement());
    }

    public LMNetworkWrapper getWrapper() {
        return ServerUtilitiesLibraryLibNetHandler.NET_GUI;
    }

    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MessageContext ctx) {
        if (ServerUtilitiesLibraryModClient.notifications.get() != EnumScreen.OFF) {
            Notification n = Notification.deserialize(JsonElementIO.read(io));

            if (ServerUtilitiesLibraryModClient.notifications.get() == EnumScreen.SCREEN) ClientNotifications.add(n);
            else {
                ServerUtilitiesLibraryClient.mc.thePlayer.addChatMessage(n.title);
                if (n.desc != null) ServerUtilitiesLibraryClient.mc.thePlayer.addChatMessage(n.desc);
            }
        }

        return null;
    }
}
