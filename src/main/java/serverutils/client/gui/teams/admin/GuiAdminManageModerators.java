package serverutils.client.gui.teams.admin;

import java.util.Collection;
import java.util.List;

import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;

import serverutils.client.gui.teams.GuiManagePlayersBase;
import serverutils.lib.EnumTeamStatus;
import serverutils.lib.gui.GuiHelper;
import serverutils.lib.gui.Panel;
import serverutils.lib.icon.Color4I;
import serverutils.lib.util.misc.MouseButton;
import serverutils.net.MessageAdminTeamAction;
import serverutils.net.MessageMyTeamPlayerList;

public class GuiAdminManageModerators extends GuiManagePlayersBase {

    private static class ButtonPlayer extends ButtonPlayerBase {

        private final String teamId;

        private ButtonPlayer(Panel panel, String teamId, MessageMyTeamPlayerList.Entry m) {
            super(panel, m);
            this.teamId = teamId;
        }

        @Override
        protected Color4I getPlayerColor() {
            return entry.status.isEqualOrGreaterThan(EnumTeamStatus.MOD)
                    ? Color4I.getChatFormattingColor(EnumChatFormatting.DARK_GREEN)
                    : getDefaultPlayerColor();
        }

        @Override
        public void addMouseOverText(List<String> list) {
            list.add(
                    I18n.format(
                            (entry.status.isEqualOrGreaterThan(EnumTeamStatus.MOD) ? EnumTeamStatus.MOD
                                    : EnumTeamStatus.MEMBER).getLangKey()));
        }

        @Override
        public void onClicked(MouseButton button) {
            GuiHelper.playClickSound();
            NBTTagCompound data = new NBTTagCompound();
            data.setString("player", entry.name);

            if (entry.status.isEqualOrGreaterThan(EnumTeamStatus.MOD)) {
                data.setBoolean("add", false);
                entry.status = EnumTeamStatus.MEMBER;
            } else {
                data.setBoolean("add", true);
                entry.status = EnumTeamStatus.MOD;
            }

            new MessageAdminTeamAction(teamId, MessageAdminTeamAction.MODERATORS, data).sendToServer();
            updateIcon();
        }
    }

    public GuiAdminManageModerators(String teamId, Collection<MessageMyTeamPlayerList.Entry> m) {
        super(
                I18n.format("team_action.serverutilities.moderators"),
                m,
                (panel, e) -> new ButtonPlayer(panel, teamId, e));
    }
}
