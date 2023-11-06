package serverutils.net;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.client.ServerUtilitiesClientConfig;
import serverutils.integration.vp.VPIntegration;
import serverutils.lib.OtherMods;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;

public class MessageJourneyMapRemove extends MessageToClient {

    private int chunkX, chunkZ, dim;

    public MessageJourneyMapRemove() {}

    public MessageJourneyMapRemove(ChunkDimPos pos) {
        this.chunkX = pos.posX;
        this.chunkZ = pos.posZ;
        this.dim = pos.dim;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.CLAIMS;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeVarInt(chunkX);
        data.writeVarInt(chunkZ);
        data.writeVarInt(dim);
    }

    @Override
    public void readData(DataIn data) {
        chunkX = data.readVarInt();
        chunkZ = data.readVarInt();
        dim = data.readVarInt();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        if (Loader.isModLoaded(OtherMods.VP) && ServerUtilitiesClientConfig.general.journeymap_overlay) {
            VPIntegration.CLAIMS.remove(new ChunkDimPos(chunkX, chunkZ, dim));
        }
    }
}
