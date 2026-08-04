package serverutils.net;

import java.util.ArrayList;
import java.util.Collection;
import java.util.OptionalInt;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.client.gui.teams.admin.GuiAdminManageClaims;
import serverutils.data.ClaimedChunk;
import serverutils.data.ClaimedChunks;
import serverutils.lib.data.ForgeTeam;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;

public class MessageAdminTeamClaimsList extends MessageToClient {

    public static class Entry {

        static final DataOut.Serializer<Entry> SERIALIZER = (data, object) -> object.writeData(data);
        static final DataIn.Deserializer<Entry> DESERIALIZER = Entry::new;

        public final int dim;
        public final int x;
        public final int z;
        public boolean loaded;

        Entry(DataIn data) {
            dim = data.readInt();
            x = data.readInt();
            z = data.readInt();
            loaded = data.readBoolean();
        }

        Entry(ClaimedChunk chunk) {
            ChunkDimPos pos = chunk.getPos();
            dim = pos.dim;
            x = pos.posX;
            z = pos.posZ;
            loaded = chunk.isLoaded();
        }

        private void writeData(DataOut data) {
            data.writeInt(dim);
            data.writeInt(x);
            data.writeInt(z);
            data.writeBoolean(loaded);
        }
    }

    private String teamId;
    private Collection<Entry> entries;

    public MessageAdminTeamClaimsList() {}

    public MessageAdminTeamClaimsList(ForgeTeam team) {
        teamId = team.getId();
        entries = new ArrayList<>();

        for (ClaimedChunk chunk : ClaimedChunks.instance.getTeamChunks(team, OptionalInt.empty(), true)) {
            entries.add(new Entry(chunk));
        }
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.GENERAL;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeString(teamId);
        data.writeCollection(entries, Entry.SERIALIZER);
    }

    @Override
    public void readData(DataIn data) {
        teamId = data.readString();
        entries = data.readCollection(Entry.DESERIALIZER);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        new GuiAdminManageClaims(teamId, entries).openGui();
    }
}
