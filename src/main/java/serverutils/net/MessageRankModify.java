package serverutils.net;

import static serverutils.ServerUtilitiesPermissions.RANK_EDIT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;

import serverutils.ServerUtilitiesConfig;
import serverutils.ServerUtilitiesPermissions;
import serverutils.client.gui.ranks.RankInst;
import serverutils.lib.config.ConfigValueInstance;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.Universe;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToServer;
import serverutils.lib.net.NetworkWrapper;
import serverutils.lib.util.permission.PermissionAPI;
import serverutils.ranks.PlayerRank;
import serverutils.ranks.Rank;
import serverutils.ranks.Ranks;

public class MessageRankModify extends MessageToServer {

    private RankInst inst;
    private Collection<String> removedEntries;

    public MessageRankModify() {}

    public MessageRankModify(RankInst rank, Collection<String> removedEntries) {
        this.inst = rank;
        this.removedEntries = removedEntries;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.FILES;
    }

    @Override
    public void writeData(DataOut data) {
        RankInst.SERIALIZER.write(data, inst);
        data.writeCollection(removedEntries, DataOut.STRING);
    }

    @Override
    public void readData(DataIn data) {
        inst = RankInst.DESERIALIZER.read(data);
        removedEntries = data.readCollection(DataIn.STRING);
    }

    @Override
    public void onMessage(EntityPlayerMP player) {
        if (!PermissionAPI.hasPermission(player, RANK_EDIT)) return;
        Rank rank = Ranks.INSTANCE.getRank(inst.getId());
        if (rank == null) {
            player.addChatMessage(new ChatComponentText("Rank: " + inst.getId() + " not found"));
            return;
        }

        boolean updateNames = false;
        boolean shouldSave = false;
        for (ConfigValueInstance value : inst.group.getValues()) {
            Rank.Entry entry = rank.setPermission(value.getId(), value.getValue());
            if (entry == null) continue;
            if (entry.node.equals(ServerUtilitiesPermissions.CHAT_NAME_FORMAT)) {
                updateNames = true;
            }
            shouldSave = true;
        }

        for (String removed : removedEntries) {
            Rank.Entry entry = rank.setPermission(removed, "");
            if (entry == null) continue;
            if (entry.node.equals(ServerUtilitiesPermissions.CHAT_NAME_FORMAT)) {
                updateNames = true;
            }
            shouldSave = true;

        }

        if (shouldSave) {
            Ranks.INSTANCE.save();
        }

        if (ServerUtilitiesConfig.chat.replace_tab_names && updateNames) {
            List<ForgePlayer> toUpdate = new ArrayList<>();
            for (PlayerRank playerRank : Ranks.INSTANCE.playerRanks.values()) {
                if (!playerRank.getParents().contains(rank)) continue;
                ForgePlayer fp = Universe.get().getPlayer(playerRank.profile);
                if (fp == null || !fp.isOnline()) continue;
                toUpdate.add(fp);
            }
            new MessageUpdateTabName(toUpdate).sendToAll();
        }
    }
}
