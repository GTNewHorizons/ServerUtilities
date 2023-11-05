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

public class GuiManageMembers extends GuiManagePlayersBase {

    private static class ButtonPlayer extends ButtonPlayerBase {

        private ButtonPlayer(Panel panel, MessageMyTeamPlayerList.Entry m) {
            super(panel, m);
        }

        @Override
        Color4I getPlayerColor() {
            if (entry.requestingInvite) {
                return Color4I.getChatFormattingColor(EnumChatFormatting.GOLD);
            }

            switch (entry.status) {
                case NONE:
                    return getDefaultPlayerColor();
                case MEMBER:
                case MOD:
                    return Color4I.getChatFormattingColor(EnumChatFormatting.DARK_GREEN);
                case INVITED:
                    return Color4I.getChatFormattingColor(EnumChatFormatting.BLUE);
                case ALLY:
                    return Color4I.getChatFormattingColor(EnumChatFormatting.DARK_AQUA);
                default:
                    break;
            }

            return getDefaultPlayerColor();
        }

        @Override
        public void addMouseOverText(List<String> list) {
            if (!entry.status.isNone()) {
                list.add(I18n.format(entry.status.getLangKey()));
            } else if (entry.requestingInvite) {
                list.add(I18n.format("serverutilities.lang.team_status.requesting_invite"));
            }

            if (entry.requestingInvite) {
                list.add(I18n.format("serverutilities.lang.team.gui.members.requesting_invite"));
            } else if (entry.status.isEqualOrGreaterThan(EnumTeamStatus.MEMBER)) {
                list.add(I18n.format("serverutilities.lang.team.gui.members.kick"));
            } else if (entry.status == EnumTeamStatus.INVITED) {
                list.add(I18n.format("serverutilities.lang.team.gui.members.cancel_invite"));
            }

            if (entry.status == EnumTeamStatus.NONE || entry.requestingInvite) {
                list.add(I18n.format("serverutilities.lang.team.gui.members.invite"));
            }

            if (entry.requestingInvite) {
                list.add(I18n.format("serverutilities.lang.team.gui.members.deny_request"));
            }
        }

        @Override
        public void onClicked(MouseButton button) {
            GuiHelper.playClickSound();
            NBTTagCompound data = new NBTTagCompound();
            data.setString("player", entry.name);

            if (entry.requestingInvite) {
                if (button.isLeft()) {
                    data.setString("action", "invite");
                    entry.status = EnumTeamStatus.MEMBER;
                } else {
                    data.setString("action", "deny_request");
                    entry.status = EnumTeamStatus.NONE;
                }

                entry.requestingInvite = false;
            } else if (entry.status == EnumTeamStatus.NONE) {
                if (button.isLeft()) {
                    data.setString("action", "invite");
                    entry.status = EnumTeamStatus.INVITED;
                }
            } else if (entry.status.isEqualOrGreaterThan(EnumTeamStatus.MEMBER)) {
                if (!button.isLeft()) {
                    data.setString("action", "kick");
                    entry.requestingInvite = true;
                    entry.status = EnumTeamStatus.NONE;
                }
            } else if (entry.status == EnumTeamStatus.INVITED) {
                if (!button.isLeft()) {
                    data.setString("action", "cancel_invite");
                    entry.status = EnumTeamStatus.NONE;
                }
            }

            if (data.hasKey("action")) {
                new MessageMyTeamAction(ServerUtilitiesTeamGuiActions.MEMBERS.getId(), data).sendToServer();
            }

            updateIcon();
        }
    }

    public GuiManageMembers(Collection<MessageMyTeamPlayerList.Entry> m) {
        super(I18n.format("team_action.serverutilities.members"), m, ButtonPlayer::new);
    }
}
