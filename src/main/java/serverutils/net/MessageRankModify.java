package serverutils.net;

import java.util.Collection;

import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.client.gui.ranks.RankInst;
import serverutils.lib.config.ConfigValueInstance;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToServer;
import serverutils.lib.net.NetworkWrapper;
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
        boolean shouldSave = false;
        Rank rank = Ranks.INSTANCE.getRank(inst.getId());
        if (rank == null) return;

        for (ConfigValueInstance value : inst.group.getValues()) {
            if (rank.setPermission(value.getId(), value.getValue()) != null) {
                shouldSave = true;
            }
        }

        for (String removed : removedEntries) {
            if (rank.setPermission(removed, "") != null) {
                shouldSave = true;
            }
        }

        if (shouldSave) {
            Ranks.INSTANCE.save();
        }
    }

}
