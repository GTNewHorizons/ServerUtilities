package serverutils.net;

import java.util.ArrayList;
import java.util.Collection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.client.gui.teams.PublicTeamData;
import serverutils.client.gui.teams.admin.GuiAdminSelectTeam;
import serverutils.lib.data.ForgeTeam;
import serverutils.lib.data.Universe;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;

public class MessageAdminTeamList extends MessageToClient {

    private Collection<PublicTeamData> teams;

    public MessageAdminTeamList() {}

    public MessageAdminTeamList(Universe universe) {
        teams = new ArrayList<>();

        for (ForgeTeam team : universe.getTeams()) {
            teams.add(new PublicTeamData(team, PublicTeamData.Type.NEEDS_INVITE));
        }
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.GENERAL;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeCollection(teams, PublicTeamData.SERIALIZER);
    }

    @Override
    public void readData(DataIn data) {
        teams = data.readCollection(null, PublicTeamData.DESERIALIZER);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        new GuiAdminSelectTeam(teams).openGui();
    }
}
