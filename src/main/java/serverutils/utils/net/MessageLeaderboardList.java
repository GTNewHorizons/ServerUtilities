package serverutils.utils.net;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;

import com.feed_the_beast.ftblib.lib.net.MessageToServer;
import com.feed_the_beast.ftblib.lib.net.NetworkWrapper;
import com.feed_the_beast.ftblib.lib.util.permission.PermissionAPI;
import serverutils.utils.ServerUtilitiesCommon;
import serverutils.utils.ServerUtilitiesPermissions;
import serverutils.utils.data.Leaderboard;

public class MessageLeaderboardList extends MessageToServer {

    @Override
    public NetworkWrapper getWrapper() {
        return FTBUtilitiesNetHandler.STATS;
    }

    @Override
    public void onMessage(EntityPlayerMP player) {
        Map<ResourceLocation, IChatComponent> map = new LinkedHashMap<>();

        for (Leaderboard leaderboard : ServerUtilitiesCommon.LEADERBOARDS.values()) {
            if (PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.getLeaderboardNode(leaderboard))) {
                map.put(leaderboard.id, leaderboard.getTitle());
            }
        }

        new MessageLeaderboardListResponse(map).sendTo(player);
    }
}
