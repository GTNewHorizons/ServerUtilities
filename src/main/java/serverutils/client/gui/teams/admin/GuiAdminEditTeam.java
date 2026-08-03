package serverutils.client.gui.teams.admin;

import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;

import serverutils.lib.gui.GuiHelper;
import serverutils.lib.gui.GuiIcons;
import serverutils.lib.gui.Panel;
import serverutils.lib.gui.SimpleTextButton;
import serverutils.lib.gui.misc.GuiButtonListBase;
import serverutils.lib.icon.Icon;
import serverutils.lib.util.misc.MouseButton;
import serverutils.net.MessageAdminTeamAction;

public class GuiAdminEditTeam extends GuiButtonListBase {

    private class ButtonAction extends SimpleTextButton {

        private final String action;

        private ButtonAction(Panel panel, String title, Icon icon, String a) {
            super(panel, title, icon);
            action = a;
        }

        @Override
        public void onClicked(MouseButton button) {
            GuiHelper.playClickSound();
            new MessageAdminTeamAction(teamId, action, new NBTTagCompound()).sendToServer();
        }
    }

    private final String teamId;

    public GuiAdminEditTeam(String teamId, String title) {
        this.teamId = teamId;
        setTitle(title);
    }

    @Override
    public void addButtons(Panel panel) {
        panel.add(
                new ButtonAction(
                        panel,
                        I18n.format("gui.settings"),
                        GuiIcons.SETTINGS,
                        MessageAdminTeamAction.SETTINGS));
        panel.add(
                new ButtonAction(
                        panel,
                        I18n.format("team_action.serverutilities.transfer_ownership"),
                        GuiIcons.RIGHT,
                        MessageAdminTeamAction.OWNER));
        panel.add(
                new ButtonAction(
                        panel,
                        I18n.format("team_action.serverutilities.moderators"),
                        GuiIcons.SHIELD,
                        MessageAdminTeamAction.MODERATORS));
        panel.add(
                new ButtonAction(
                        panel,
                        I18n.format("team_action.serverutilities.members"),
                        GuiIcons.FRIENDS,
                        MessageAdminTeamAction.MEMBERS));
        panel.add(
                new ButtonAction(
                        panel,
                        I18n.format("serverutilities.admin_panel.claimed_chunks"),
                        GuiIcons.MAP,
                        MessageAdminTeamAction.CLAIMS));
    }
}
