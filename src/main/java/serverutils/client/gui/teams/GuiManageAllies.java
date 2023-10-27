package serverutils.client.gui.teams;

import java.util.Collection;
import java.util.List;

import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;

import serverutils.lib.EnumTeamStatus;
import serverutils.lib.data.ServerUtilitiesTeamGuiActions;
import serverutils.lib.gui.GuiHelper;
import serverutils.lib.gui.Panel;
import serverutils.lib.icon.Color4I;
import serverutils.lib.util.misc.MouseButton;
import serverutils.net.MessageMyTeamAction;
import serverutils.net.MessageMyTeamPlayerList;

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

            new MessageMyTeamAction(ServerUtilitiesTeamGuiActions.ALLIES.getId(), data).sendToServer();
            updateIcon();
        }
    }

    public GuiManageAllies(Collection<MessageMyTeamPlayerList.Entry> m) {
        super(I18n.format("team_action.serverutilities.allies"), m, ButtonPlayer::new);
    }
}
