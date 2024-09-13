package serverutils.net;

import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.ServerUtilitiesPermissions;
import serverutils.data.ClaimedChunks;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToServer;
import serverutils.lib.net.NetworkWrapper;
import serverutils.lib.util.permission.PermissionAPI;

public class MessageNavigatorRequest extends MessageToServer {

    private int minX, maxX, minZ, maxZ;

    public MessageNavigatorRequest() {}

    public MessageNavigatorRequest(int minX, int maxX, int minZ, int maxZ) {
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.CLAIMS;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeVarInt(minX);
        data.writeVarInt(maxX);
        data.writeVarInt(minZ);
        data.writeVarInt(maxZ);
    }

    @Override
    public void readData(DataIn data) {
        minX = data.readVarInt();
        maxX = data.readVarInt();
        minZ = data.readVarInt();
        maxZ = data.readVarInt();
    }

    @Override
    public void onMessage(EntityPlayerMP player) {
        if (ClaimedChunks.isActive()
                && PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.CLAIMS_JOURNEYMAP)) {
            new MessageNavigatorUpdate(minX, maxX, minZ, maxZ, player).sendTo(player);
        }
    }
}
