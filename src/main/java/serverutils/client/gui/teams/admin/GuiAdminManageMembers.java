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

public class GuiAdminManageMembers extends GuiManagePlayersBase {

    private static class ButtonPlayer extends ButtonPlayerBase {

        private final String teamId;

        private ButtonPlayer(Panel panel, String teamId, MessageMyTeamPlayerList.Entry m) {
            super(panel, m);
            this.teamId = teamId;
        }

        private boolean isMember() {
            return entry.status.isEqualOrGreaterThan(EnumTeamStatus.MEMBER);
        }

        @Override
        protected Color4I getPlayerColor() {
            return isMember() ? Color4I.getChatFormattingColor(EnumChatFormatting.DARK_GREEN) : getDefaultPlayerColor();
        }

        @Override
        public void addMouseOverText(List<String> list) {
            if (isMember()) {
                list.add(I18n.format("serverutilities.lang.team.gui.members.kick"));
            } else {
                list.add(I18n.format("serverutilities.lang.team.gui.members.invite"));
            }
        }

        @Override
        public void onClicked(MouseButton button) {
            GuiHelper.playClickSound();
            NBTTagCompound data = new NBTTagCompound();
            data.setString("player", entry.name);

            if (isMember()) {
                if (!button.isLeft()) {
                    data.setBoolean("add", false);
                    entry.status = EnumTeamStatus.NONE;
                } else {
                    return;
                }
            } else {
                if (button.isLeft()) {
                    data.setBoolean("add", true);
                    entry.status = EnumTeamStatus.MEMBER;
                } else {
                    return;
                }
            }

            new MessageAdminTeamAction(teamId, MessageAdminTeamAction.MEMBERS, data).sendToServer();
            updateIcon();
        }
    }

    public GuiAdminManageMembers(String teamId, Collection<MessageMyTeamPlayerList.Entry> m) {
        super(I18n.format("team_action.serverutilities.members"), m, (panel, e) -> new ButtonPlayer(panel, teamId, e));
    }
}
