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

            new MessageMyTeamAction(ServerUtilitiesTeamGuiActions.ENEMIES.getId(), data).sendToServer();
            updateIcon();
        }
    }

    public GuiManageEnemies(Collection<MessageMyTeamPlayerList.Entry> m) {
        super(I18n.format("team_action.serverutilities.enemies"), m, ButtonPlayer::new);
    }
}
