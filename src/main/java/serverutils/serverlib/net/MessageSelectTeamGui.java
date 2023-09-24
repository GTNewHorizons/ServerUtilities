package serverutils.serverlib.net;

import java.util.ArrayList;
import java.util.Collection;

import serverutils.serverlib.client.teamsgui.GuiSelectTeam;
import serverutils.serverlib.client.teamsgui.PublicTeamData;
import serverutils.serverlib.lib.data.ForgePlayer;
import serverutils.serverlib.lib.data.ForgeTeam;
import serverutils.serverlib.lib.data.Universe;
import serverutils.serverlib.lib.io.DataIn;
import serverutils.serverlib.lib.io.DataOut;
import serverutils.serverlib.lib.net.MessageToClient;
import serverutils.serverlib.lib.net.NetworkWrapper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class MessageSelectTeamGui extends MessageToClient {

	private Collection<PublicTeamData> teams;
	private boolean canCreate;

	public MessageSelectTeamGui() {}

	public MessageSelectTeamGui(ForgePlayer player, boolean c) {
		teams = new ArrayList<>();

		for (ForgeTeam team : Universe.get().getTeams()) {
			PublicTeamData.Type type = PublicTeamData.Type.NEEDS_INVITE;

			if (team.isEnemy(player)) {
				type = PublicTeamData.Type.ENEMY;
			} else if (team.isInvited(player)) {
				type = PublicTeamData.Type.CAN_JOIN;
			} else if (team.isRequestingInvite(player)) {
				type = PublicTeamData.Type.REQUESTING_INVITE;
			}

			teams.add(new PublicTeamData(team, type));
		}

		canCreate = c;
	}

	@Override
	public NetworkWrapper getWrapper() {
		return ServerLibNetHandler.MY_TEAM;
	}

	@Override
	public void writeData(DataOut data) {
		data.writeCollection(teams, PublicTeamData.SERIALIZER);
		data.writeBoolean(canCreate);
	}

	@Override
	public void readData(DataIn data) {
		teams = data.readCollection(null, PublicTeamData.DESERIALIZER);
		canCreate = data.readBoolean();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void onMessage() {
		new GuiSelectTeam(teams, canCreate).openGui();
	}
}
