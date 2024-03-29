package serverutils.net;

import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.ServerUtilitiesConfig;
import serverutils.ServerUtilitiesPermissions;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToServer;
import serverutils.lib.net.NetworkWrapper;
import serverutils.lib.util.permission.PermissionAPI;

public class MessageJourneyMapRequest extends MessageToServer {

    private int minX, maxX, minZ, maxZ;

    public MessageJourneyMapRequest() {}

    public MessageJourneyMapRequest(int minX, int maxX, int minZ, int maxZ) {
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
        if (ServerUtilitiesConfig.world.chunk_claiming
                && PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.CLAIMS_JOURNEYMAP)) {
            new MessageJourneyMapUpdate(minX, maxX, minZ, maxZ, player).sendTo(player);
        }
    }
}
