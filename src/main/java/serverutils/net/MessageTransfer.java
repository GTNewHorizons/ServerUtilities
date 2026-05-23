package serverutils.net;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.client.TransferClientHandler;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;

public class MessageTransfer extends MessageToClient {

    private String host;
    private int port;

    public MessageTransfer() {}

    public MessageTransfer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.GENERAL;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeString(host);
        data.writeInt(port);
    }

    @Override
    public void readData(DataIn data) {
        host = data.readString();
        port = data.readInt();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        if (!ServerUtilitiesConfig.transfer.enabled) {
            ServerUtilities.LOGGER.info("Ignoring transfer request to {}:{} (disabled in config)", host, port);
            return;
        }

        if (host == null || host.isEmpty() || port < 1 || port > 65535) {
            ServerUtilities.LOGGER.warn("Ignoring invalid transfer request: host={}, port={}", host, port);
            return;
        }

        if (!isHostAllowed(host, port)) {
            ServerUtilities.LOGGER.warn("Ignoring transfer request to {}:{} (not in whitelist)", host, port);
            return;
        }

        ServerUtilities.LOGGER.info("Server requested transfer to {}:{}", host, port);
        TransferClientHandler.execute(host, port);
    }

    private static boolean isHostAllowed(String host, int port) {
        String[] whitelist = ServerUtilitiesConfig.transfer.whitelist;
        if (whitelist == null || whitelist.length == 0) {
            return true;
        }
        String hostPort = host + ":" + port;
        for (String entry : whitelist) {
            if (entry.equalsIgnoreCase(host) || entry.equalsIgnoreCase(hostPort)) {
                return true;
            }
        }
        return false;
    }
}
