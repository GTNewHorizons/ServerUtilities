package serverutils.net;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.client.ServerUtilitiesClientConfig;
import serverutils.integration.navigator.NavigatorIntegration;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;

public class MessageJourneyMapRemove extends MessageToClient {

    private long chunkPos;

    public MessageJourneyMapRemove() {}

    public MessageJourneyMapRemove(long pos) {
        this.chunkPos = pos;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.CLAIMS;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeLong(chunkPos);
    }

    @Override
    public void readData(DataIn data) {
        chunkPos = data.readLong();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        if (ServerUtilitiesClientConfig.general.journeymap_overlay) {
            NavigatorIntegration.removeChunk(chunkPos);
        }
    }
}
