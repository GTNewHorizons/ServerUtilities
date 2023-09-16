package ftb.lib.mod.net;

import cpw.mods.fml.common.network.simpleimpl.*;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ftb.lib.EnumScreen;
import ftb.lib.api.client.FTBLibClient;
import ftb.lib.api.net.LMNetworkWrapper;
import ftb.lib.api.net.MessageLM;
import ftb.lib.api.notification.ClientNotifications;
import ftb.lib.api.notification.Notification;
import ftb.lib.mod.client.FTBLibModClient;
import latmod.lib.ByteCount;
import latmod.lib.json.JsonElementIO;

public class MessageNotifyPlayer extends MessageLM {

    public MessageNotifyPlayer() {
        super(ByteCount.SHORT);
    }

    public MessageNotifyPlayer(Notification n) {
        this();
        JsonElementIO.write(io, n.getSerializableElement());
    }

    public LMNetworkWrapper getWrapper() {
        return FTBLibNetHandler.NET_GUI;
    }

    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MessageContext ctx) {
        if (FTBLibModClient.notifications.get() != EnumScreen.OFF) {
            Notification n = Notification.deserialize(JsonElementIO.read(io));

            if (FTBLibModClient.notifications.get() == EnumScreen.SCREEN) ClientNotifications.add(n);
            else {
                FTBLibClient.mc.thePlayer.addChatMessage(n.title);
                if (n.desc != null) FTBLibClient.mc.thePlayer.addChatMessage(n.desc);
            }
        }

        return null;
    }
}
