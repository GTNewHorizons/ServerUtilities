package serverutils.net;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Predicate;

import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.client.gui.teams.GuiManageAllies;
import serverutils.client.gui.teams.GuiManageEnemies;
import serverutils.client.gui.teams.GuiManageMembers;
import serverutils.client.gui.teams.GuiManageModerators;
import serverutils.client.gui.teams.GuiTransferOwnership;
import serverutils.lib.EnumTeamStatus;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.ServerUtilitiesTeamGuiActions;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;

public class MessageMyTeamPlayerList extends MessageToClient {

    public static class Entry implements Comparable<Entry> {

        private static final DataOut.Serializer<Entry> SERIALIZER = (data, object) -> object.writeData(data);
        private static final DataIn.Deserializer<Entry> DESERIALIZER = Entry::new;

        public final UUID uuid;
        public final String name;
        public EnumTeamStatus status;
        public boolean requestingInvite;

        private Entry(DataIn data) {
            uuid = data.readUUID();
            name = data.readString();
            status = EnumTeamStatus.NAME_MAP.read(data);
            requestingInvite = data.readBoolean();
        }

        public Entry(ForgePlayer player, EnumTeamStatus s, boolean i) {
            uuid = player.getId();
            name = player.getDisplayNameString();
            status = s;
            requestingInvite = i;
        }

        private void writeData(DataOut data) {
            data.writeUUID(uuid);
            data.writeString(name);
            EnumTeamStatus.NAME_MAP.write(data, status);
            data.writeBoolean(requestingInvite);
        }

        public int getSortIndex() {
            return requestingInvite ? 1000 : Math.max(status.getStatus(), status == EnumTeamStatus.ENEMY ? 1 : 0);
        }

        @Override
        public int compareTo(Entry o) {
            int o1s = getSortIndex();
            int o2s = o.getSortIndex();
            return o1s == o2s ? name.compareToIgnoreCase(o.name) : o2s - o1s;
        }
    }

    private ResourceLocation id;
    private Collection<Entry> entries;

    public MessageMyTeamPlayerList() {}

    public MessageMyTeamPlayerList(ResourceLocation _id, ForgePlayer player, Predicate<EnumTeamStatus> predicate) {
        id = _id;
        entries = new ArrayList<>();

        for (ForgePlayer p : player.team.universe.getPlayers()) {
            if (p != player) {
                EnumTeamStatus status = player.team.getHighestStatus(p);

                if (status != EnumTeamStatus.OWNER && predicate.test(status)) {
                    entries.add(new Entry(p, status, player.team.isRequestingInvite(p)));
                }
            }
        }
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.MY_TEAM;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeResourceLocation(id);
        data.writeCollection(entries, Entry.SERIALIZER);
    }

    @Override
    public void readData(DataIn data) {
        id = data.readResourceLocation();
        entries = data.readCollection(Entry.DESERIALIZER);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        if (id.equals(ServerUtilitiesTeamGuiActions.MEMBERS.getId())) {
            new GuiManageMembers(entries).openGui();
        } else if (id.equals(ServerUtilitiesTeamGuiActions.ALLIES.getId())) {
            new GuiManageAllies(entries).openGui();
        } else if (id.equals(ServerUtilitiesTeamGuiActions.MODERATORS.getId())) {
            new GuiManageModerators(entries).openGui();
        } else if (id.equals(ServerUtilitiesTeamGuiActions.ENEMIES.getId())) {
            new GuiManageEnemies(entries).openGui();
        } else if (id.equals(ServerUtilitiesTeamGuiActions.TRANSFER_OWNERSHIP.getId())) {
            new GuiTransferOwnership(entries).openGui();
        }
    }
}
