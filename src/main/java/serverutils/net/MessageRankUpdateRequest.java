package serverutils.net;

import static serverutils.ServerUtilitiesPermissions.RANK_EDIT;

import java.util.ArrayList;
import java.util.Collection;

import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToServer;
import serverutils.lib.net.NetworkWrapper;
import serverutils.lib.util.permission.PermissionAPI;
import serverutils.ranks.Rank;
import serverutils.ranks.Ranks;

public class MessageRankUpdateRequest extends MessageToServer {

    private Collection<String> ranks;

    public MessageRankUpdateRequest() {}

    public MessageRankUpdateRequest(Collection<String> ranks) {
        this.ranks = ranks;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.FILES;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeCollection(ranks, DataOut.STRING);
    }

    @Override
    public void readData(DataIn data) {
        ranks = data.readCollection(DataIn.STRING);
    }

    @Override
    public void onMessage(EntityPlayerMP player) {
        if (!PermissionAPI.hasPermission(player, RANK_EDIT)) return;
        Collection<Rank> r = new ArrayList<>();
        for (String id : ranks) {
            Rank rank = Ranks.INSTANCE.getRank(id);
            if (rank == null) return;
            r.add(rank);
        }
        new MessageRankUpdateResponse(r, player).sendTo(player);
    }

}
