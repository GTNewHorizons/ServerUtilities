package serverutils.net;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;

import serverutils.ServerUtilitiesLeaderboards;
import serverutils.ServerUtilitiesPermissions;
import serverutils.data.Leaderboard;
import serverutils.lib.net.MessageToServer;
import serverutils.lib.net.NetworkWrapper;
import serverutils.lib.util.permission.PermissionAPI;

public class MessageLeaderboardList extends MessageToServer {

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.STATS;
    }

    @Override
    public void onMessage(EntityPlayerMP player) {
        Map<ResourceLocation, IChatComponent> map = new LinkedHashMap<>();

        for (Leaderboard leaderboard : ServerUtilitiesLeaderboards.LEADERBOARDS.values()) {
            if (PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.getLeaderboardNode(leaderboard))) {
                map.put(leaderboard.id, leaderboard.getTitle());
            }
        }

        new MessageLeaderboardListResponse(map).sendTo(player);
    }
}
