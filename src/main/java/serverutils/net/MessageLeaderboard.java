package serverutils.net;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;

import serverutils.ServerUtilitiesLeaderboards;
import serverutils.ServerUtilitiesPermissions;
import serverutils.data.Leaderboard;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToServer;
import serverutils.lib.net.NetworkWrapper;
import serverutils.lib.util.permission.PermissionAPI;

public class MessageLeaderboard extends MessageToServer {

    private ResourceLocation id;

    public MessageLeaderboard() {}

    public MessageLeaderboard(ResourceLocation _id) {
        id = _id;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.STATS;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeResourceLocation(id);
    }

    @Override
    public void readData(DataIn data) {
        id = data.readResourceLocation();
    }

    @Override
    public void onMessage(EntityPlayerMP player) {
        Leaderboard leaderboard = ServerUtilitiesLeaderboards.LEADERBOARDS.get(id);

        if (leaderboard != null
                && PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.getLeaderboardNode(leaderboard))) {
            new MessageLeaderboardResponse(player, leaderboard).sendTo(player);
        }
    }
}
