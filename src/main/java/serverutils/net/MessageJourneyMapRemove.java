package serverutils.net;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.integration.navigator.NavigatorIntegration;
import serverutils.lib.OtherMods;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;

public class MessageJourneyMapRemove extends MessageToClient {

    private ChunkDimPos chunkPos;

    public MessageJourneyMapRemove() {}

    public MessageJourneyMapRemove(ChunkDimPos pos) {
        this.chunkPos = pos;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.CLAIMS;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeChunkDimPos(chunkPos);
    }

    @Override
    public void readData(DataIn data) {
        chunkPos = data.readChunkDimPos();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        if(!OtherMods.isNavigatorLoaded()) return;
        NavigatorIntegration.removeChunk(chunkPos.posX, chunkPos.posZ, chunkPos.dim);
    }
}
