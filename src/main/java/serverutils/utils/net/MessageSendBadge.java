package serverutils.utils.net;

import java.util.UUID;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.lib.io.DataIn;
import serverutils.lib.lib.io.DataOut;
import serverutils.lib.lib.net.MessageToClient;
import serverutils.lib.lib.net.NetworkWrapper;
import serverutils.utils.handlers.ServerUtilitiesClientEventHandler;

public class MessageSendBadge extends MessageToClient {

    private UUID playerId;
    private String badgeURL;

    public MessageSendBadge() {}

    public MessageSendBadge(UUID player, String url) {
        playerId = player;
        badgeURL = url;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.GENERAL;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeUUID(playerId);
        data.writeString(badgeURL);
    }

    @Override
    public void readData(DataIn data) {
        playerId = data.readUUID();
        badgeURL = data.readString();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        ServerUtilitiesClientEventHandler.setBadge(playerId, badgeURL);
    }
}
