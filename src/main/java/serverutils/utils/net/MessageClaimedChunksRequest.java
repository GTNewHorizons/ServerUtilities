package serverutils.utils.net;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.lib.lib.gui.misc.ChunkSelectorMap;
import serverutils.lib.lib.io.DataIn;
import serverutils.lib.lib.io.DataOut;
import serverutils.lib.lib.math.MathUtils;
import serverutils.lib.lib.net.MessageToServer;
import serverutils.lib.lib.net.NetworkWrapper;
import serverutils.utils.data.ClaimedChunks;

public class MessageClaimedChunksRequest extends MessageToServer {

    private int startX, startZ;

    public MessageClaimedChunksRequest() {}

    public MessageClaimedChunksRequest(int sx, int sz) {
        startX = sx;
        startZ = sz;
    }

    public MessageClaimedChunksRequest(Entity entity) {
        this(
                MathUtils.chunk(entity.posX) - ChunkSelectorMap.TILES_GUI2,
                MathUtils.chunk(entity.posZ) - ChunkSelectorMap.TILES_GUI2);
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.CLAIMS;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeVarInt(startX);
        data.writeVarInt(startZ);
    }

    @Override
    public void readData(DataIn data) {
        startX = data.readVarInt();
        startZ = data.readVarInt();
    }

    @Override
    public void onMessage(EntityPlayerMP player) {
        if (ClaimedChunks.isActive()) {
            new MessageClaimedChunksUpdate(startX, startZ, player).sendTo(player);
        }
    }
}
