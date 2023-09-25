package serverutils.utils.net;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;

import serverutils.lib.lib.io.DataIn;
import serverutils.lib.lib.io.DataOut;
import serverutils.lib.lib.net.MessageToServer;
import serverutils.lib.lib.net.NetworkWrapper;
import serverutils.lib.lib.util.permission.PermissionAPI;
import serverutils.utils.ServerUtilitiesCommon;
import serverutils.utils.ServerUtilitiesPermissions;
import serverutils.utils.data.Leaderboard;

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
        Leaderboard leaderboard = ServerUtilitiesCommon.LEADERBOARDS.get(id);

        if (leaderboard != null
                && PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.getLeaderboardNode(leaderboard))) {
            new MessageLeaderboardResponse(player, leaderboard).sendTo(player);
        }
    }
}
