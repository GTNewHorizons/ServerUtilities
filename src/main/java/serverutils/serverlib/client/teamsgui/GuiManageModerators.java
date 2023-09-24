package serverutils.serverlib.client.teamsgui;

import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import serverutils.serverlib.lib.EnumTeamStatus;
import serverutils.serverlib.lib.data.ServerLibTeamGuiActions;
import serverutils.serverlib.lib.gui.GuiHelper;
import serverutils.serverlib.lib.gui.Panel;
import serverutils.serverlib.lib.icon.Color4I;
import serverutils.serverlib.lib.util.misc.MouseButton;
import serverutils.serverlib.net.MessageMyTeamAction;
import serverutils.serverlib.net.MessageMyTeamPlayerList;

import java.util.Collection;
import java.util.List;

public class GuiManageModerators extends GuiManagePlayersBase {
	private static class ButtonPlayer extends ButtonPlayerBase {
		private ButtonPlayer(Panel panel, MessageMyTeamPlayerList.Entry m)
		{
			super(panel, m);
		}

		@Override
        Color4I getPlayerColor() {
			return entry.status.isEqualOrGreaterThan(EnumTeamStatus.MOD) ? Color4I.getChatFormattingColor(EnumChatFormatting.DARK_GREEN) : getDefaultPlayerColor();
		}

		@Override
		public void addMouseOverText(List<String> list) {
			list.add(I18n.format((entry.status.isEqualOrGreaterThan(EnumTeamStatus.MOD) ? EnumTeamStatus.MOD : EnumTeamStatus.MEMBER).getLangKey()));
		}

		@Override
		public void onClicked(MouseButton button) {
			GuiHelper.playClickSound();
			NBTTagCompound data = new NBTTagCompound();
			data.setString("player", entry.name);

			if (entry.status.isEqualOrGreaterThan(EnumTeamStatus.MOD)) {
				data.setBoolean("add", false);
				entry.status = EnumTeamStatus.MEMBER;
			}
			else {
				data.setBoolean("add", true);
				entry.status = EnumTeamStatus.MOD;
			}
			new MessageMyTeamAction(ServerLibTeamGuiActions.MODERATORS.getId(), data).sendToServer();
			updateIcon();
		}
	}

	public GuiManageModerators(Collection<MessageMyTeamPlayerList.Entry> m)
	{
		super(I18n.format("team_action.serverlib.moderators"), m, ButtonPlayer::new);
	}
}