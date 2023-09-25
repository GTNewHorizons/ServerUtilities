package serverutils.utils.net;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;

import com.feed_the_beast.ftblib.lib.io.DataIn;
import com.feed_the_beast.ftblib.lib.io.DataOut;
import com.feed_the_beast.ftblib.lib.net.MessageToServer;
import com.feed_the_beast.ftblib.lib.net.NetworkWrapper;
import com.feed_the_beast.ftblib.lib.util.permission.PermissionAPI;
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
        return FTBUtilitiesNetHandler.STATS;
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
