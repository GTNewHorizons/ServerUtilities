package serverutils.net;

import net.minecraft.entity.player.EntityPlayerMP;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import serverutils.ServerUtilitiesPermissions;
import serverutils.data.ClaimedChunk;
import serverutils.data.ClaimedChunks;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.net.MessageToServer;
import serverutils.lib.net.NetworkWrapper;
import serverutils.lib.util.permission.PermissionAPI;

public class MessageNavigatorValidateKnown extends MessageToServer {

    private LongSet knownPositions;

    public MessageNavigatorValidateKnown() {}

    public MessageNavigatorValidateKnown(LongSet knownPositions) {
        this.knownPositions = knownPositions;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.CLAIMS;
    }

    @Override
    public void writeData(DataOut data) {
        for (long pos : knownPositions) {
            data.writeLong(pos);
        }
    }

    @Override
    public void readData(DataIn data) {
        knownPositions = new LongOpenHashSet();
        while (data.isReadable()) {
            knownPositions.add(data.readLong());
        }
    }

    @Override
    public void onMessage(EntityPlayerMP player) {
        if (knownPositions.isEmpty()) return;
        if (ClaimedChunks.isActive()
                && PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.CLAIMS_JOURNEYMAP)) {
            LongSet toRemove = new LongOpenHashSet();
            ChunkDimPos mut = new ChunkDimPos();
            for (long pos : knownPositions) {
                ClaimedChunk chunk = ClaimedChunks.instance.getChunk(mut.set(pos, player.dimension));

                if (chunk == null) {
                    toRemove.add(pos);
                }
            }
            if (toRemove.isEmpty()) return;
            new MessageNavigatorUpdateKnown(toRemove).sendTo(player);
        }
    }
}
