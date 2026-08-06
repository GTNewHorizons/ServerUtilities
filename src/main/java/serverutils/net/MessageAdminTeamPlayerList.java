package serverutils.net;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.client.gui.teams.admin.GuiAdminManageMembers;
import serverutils.client.gui.teams.admin.GuiAdminManageModerators;
import serverutils.client.gui.teams.admin.GuiAdminSetOwner;
import serverutils.lib.EnumTeamStatus;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.ForgeTeam;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;

public class MessageAdminTeamPlayerList extends MessageToClient {

    private String teamId;
    private String mode;
    private Collection<MessageMyTeamPlayerList.Entry> entries;

    public MessageAdminTeamPlayerList() {}

    public MessageAdminTeamPlayerList(ForgeTeam team, String mode, Predicate<EnumTeamStatus> predicate) {
        this.teamId = team.getId();
        this.mode = mode;
        this.entries = new ArrayList<>();

        for (ForgePlayer p : team.universe.getPlayers()) {
            EnumTeamStatus status = team.getHighestStatus(p);

            if (status != EnumTeamStatus.OWNER && predicate.test(status)) {
                entries.add(new MessageMyTeamPlayerList.Entry(p, status, false));
            }
        }
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.GENERAL;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeString(teamId);
        data.writeString(mode);
        data.writeCollection(entries, MessageMyTeamPlayerList.Entry.SERIALIZER);
    }

    @Override
    public void readData(DataIn data) {
        teamId = data.readString();
        mode = data.readString();
        entries = data.readCollection(MessageMyTeamPlayerList.Entry.DESERIALIZER);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        switch (mode) {
            case MessageAdminTeamAction.OWNER -> new GuiAdminSetOwner(teamId, entries).openGui();
            case MessageAdminTeamAction.MODERATORS -> new GuiAdminManageModerators(teamId, entries).openGui();
            case MessageAdminTeamAction.MEMBERS -> new GuiAdminManageMembers(teamId, entries).openGui();
        }
    }
}
