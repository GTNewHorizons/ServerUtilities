package serverutils.lib.client.teamsgui;

import java.util.Collection;
import java.util.List;

import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;

import serverutils.lib.lib.EnumTeamStatus;
import serverutils.lib.lib.data.ServerUtilitiesLibTeamGuiActions;
import serverutils.lib.lib.gui.GuiHelper;
import serverutils.lib.lib.gui.Panel;
import serverutils.lib.lib.icon.Color4I;
import serverutils.lib.lib.util.misc.MouseButton;
import serverutils.lib.net.MessageMyTeamAction;
import serverutils.lib.net.MessageMyTeamPlayerList;

public class GuiManageAllies extends GuiManagePlayersBase {

	private static class ButtonPlayer extends ButtonPlayerBase {

		private ButtonPlayer(Panel panel, MessageMyTeamPlayerList.Entry m) {
			super(panel, m);
		}

		@Override
        Color4I getPlayerColor() {
			return entry.status.isEqualOrGreaterThan(EnumTeamStatus.ALLY)
					? Color4I.getChatFormattingColor(EnumChatFormatting.DARK_AQUA)
					: getDefaultPlayerColor();
		}

		@Override
		public void addMouseOverText(List<String> list) {
			list.add(
					I18n.format(
							(entry.status.isEqualOrGreaterThan(EnumTeamStatus.ALLY) ? EnumTeamStatus.ALLY
									: EnumTeamStatus.MEMBER).getLangKey()));
		}

		@Override
		public void onClicked(MouseButton button) {
			GuiHelper.playClickSound();
			NBTTagCompound data = new NBTTagCompound();
			data.setString("player", entry.name);

			if (entry.status.isEqualOrGreaterThan(EnumTeamStatus.ALLY)) {
				data.setBoolean("add", false);
				entry.status = EnumTeamStatus.NONE;
			} else {
				data.setBoolean("add", true);
				entry.status = EnumTeamStatus.ALLY;
			}

			new MessageMyTeamAction(ServerUtilitiesLibTeamGuiActions.ALLIES.getId(), data).sendToServer();
			updateIcon();
		}
	}

	public GuiManageAllies(Collection<MessageMyTeamPlayerList.Entry> m) {
		super(I18n.format("team_action.ftblib.allies"), m, ButtonPlayer::new);
	}
}