package serverutils.net;

import net.minecraft.entity.player.EntityPlayerMP;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import serverutils.ServerUtilitiesPermissions;
import serverutils.data.ClaimedChunks;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
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
        if (ClaimedChunks.isActive()
                && PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.CLAIMS_JOURNEYMAP)) {
            new MessageNavigatorUpdateKnown(knownPositions, player.dimension).sendTo(player);
        }
    }
}
