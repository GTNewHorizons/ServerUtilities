package serverutils.lib.net;

import java.util.ArrayList;
import java.util.Collection;

import serverutils.lib.lib.data.ForgePlayer;
import serverutils.lib.lib.data.ForgeTeam;
import serverutils.lib.lib.data.Universe;
import serverutils.lib.lib.io.DataIn;
import serverutils.lib.lib.io.DataOut;
import serverutils.lib.lib.net.MessageToClient;
import serverutils.lib.lib.net.NetworkWrapper;
import serverutils.lib.client.teamsgui.GuiSelectTeam;
import serverutils.lib.client.teamsgui.PublicTeamData;

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
