package serverutils.serverlib.client.teamsgui;

import java.util.Collection;
import java.util.List;

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

/**
 * @author LatvianModder
 */
public class GuiManageEnemies extends GuiManagePlayersBase {

	private static class ButtonPlayer extends ButtonPlayerBase {

		private ButtonPlayer(Panel panel, MessageMyTeamPlayerList.Entry m) {
			super(panel, m);
		}

		@Override
		Color4I getPlayerColor() {
			return entry.status == EnumTeamStatus.ENEMY ? Color4I.getChatFormattingColor(EnumChatFormatting.RED)
					: getDefaultPlayerColor();
		}

		@Override
		public void addMouseOverText(List<String> list) {
			list.add(
					I18n.format(
							(entry.status == EnumTeamStatus.ENEMY ? EnumTeamStatus.ENEMY : EnumTeamStatus.NONE)
									.getLangKey()));
		}

		@Override
		public void onClicked(MouseButton button) {
			GuiHelper.playClickSound();
			NBTTagCompound data = new NBTTagCompound();
			data.setString("player", entry.name);

			if (entry.status == EnumTeamStatus.ENEMY) {
				data.setBoolean("add", false);
				entry.status = EnumTeamStatus.NONE;
			} else {
				data.setBoolean("add", true);
				entry.status = EnumTeamStatus.ENEMY;
			}

			new MessageMyTeamAction(ServerLibTeamGuiActions.ENEMIES.getId(), data).sendToServer();
			updateIcon();
		}
	}

	public GuiManageEnemies(Collection<MessageMyTeamPlayerList.Entry> m) {
		super(I18n.format("team_action.serverlib.enemies"), m, ButtonPlayer::new);
	}
}