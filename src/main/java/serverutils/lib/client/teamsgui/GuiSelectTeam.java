package serverutils.lib.client.teamsgui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;

import serverutils.lib.lib.EnumTeamStatus;
import serverutils.lib.lib.client.ClientUtils;
import serverutils.lib.lib.gui.GuiHelper;
import serverutils.lib.lib.gui.GuiIcons;
import serverutils.lib.lib.gui.Panel;
import serverutils.lib.lib.gui.SimpleTextButton;
import serverutils.lib.lib.gui.WidgetType;
import serverutils.lib.lib.gui.misc.GuiButtonListBase;
import serverutils.lib.lib.util.misc.MouseButton;

/**
 * @author LatvianModder
 */
public class GuiSelectTeam extends GuiButtonListBase {

    private static class ButtonCreateTeam extends SimpleTextButton {

        private final boolean canCreate;

        private ButtonCreateTeam(Panel panel, boolean c) {
            super(panel, I18n.format("team_action.serverlib.create_team"), GuiIcons.ADD);
            canCreate = c;
        }

        @Override
        public void onClicked(MouseButton button) {
            GuiHelper.playClickSound();
            new GuiCreateTeam().openGui();
        }

        @Override
        public WidgetType getWidgetType() {
            return canCreate ? WidgetType.mouseOver(isMouseOver()) : WidgetType.DISABLED;
        }
    }

    private static class ButtonTeam extends SimpleTextButton {

        private final PublicTeamData team;

        private ButtonTeam(Panel panel, PublicTeamData t) {
            super(panel, t.displayName.getUnformattedText(), t.icon.withBorder(t.color.getColor(), false));
            team = t;

            if (team.type == PublicTeamData.Type.REQUESTING_INVITE) {
                setTitle(EnumChatFormatting.AQUA + getTitle());
            } else if (team.type == PublicTeamData.Type.ENEMY) {
                setTitle(EnumChatFormatting.RED + getTitle());
            } else if (team.type == PublicTeamData.Type.CAN_JOIN) {
                setTitle(EnumChatFormatting.GREEN + getTitle());
            }
        }

        @Override
        public void onClicked(MouseButton button) {
            GuiHelper.playClickSound();

            if (team.type == PublicTeamData.Type.CAN_JOIN) {
                ClientUtils.execClientCommand("/team join " + team.getId());
                getGui().closeGui();
            } else if (team.type != PublicTeamData.Type.ENEMY && team.type != PublicTeamData.Type.REQUESTING_INVITE) {
                ClientUtils.execClientCommand("/team request_invite " + team.getId());
                team.type = PublicTeamData.Type.REQUESTING_INVITE;
                setTitle(EnumChatFormatting.AQUA + getTitle());
                parent.alignWidgets();
            }
        }

        @Override
        public void addMouseOverText(List<String> list) {
            if (!team.description.isEmpty()) {
                list.add(EnumChatFormatting.ITALIC + team.description);
            }

            if (team.type == PublicTeamData.Type.REQUESTING_INVITE) {
                list.add(EnumChatFormatting.GRAY + I18n.format("serverlib.lang.team_status.requesting_invite"));
            } else if (team.type == PublicTeamData.Type.ENEMY) {
                list.add(EnumChatFormatting.GRAY + I18n.format(EnumTeamStatus.ENEMY.getLangKey()));
            } else {
                list.add(
                        EnumChatFormatting.GRAY + I18n.format(
                                team.type == PublicTeamData.Type.CAN_JOIN ? "serverlib.lang.team.gui.join_team"
                                        : "serverlib.lang.team.gui.request_invite",
                                team.color.getEnumChatFormatting() + team.getId() + EnumChatFormatting.GRAY));
            }
        }

        @Override
        public WidgetType getWidgetType() {
            return team.type == PublicTeamData.Type.ENEMY ? WidgetType.DISABLED : WidgetType.mouseOver(isMouseOver());
        }
    }

    private final boolean canCreate;
    private final List<PublicTeamData> teams;

    public GuiSelectTeam(Collection<PublicTeamData> teams0, boolean c) {
        setTitle(I18n.format("team_action.serverlib.select_team"));
        setHasSearchBox(true);
        teams = new ArrayList<>(teams0);
        teams.sort(null);
        canCreate = c;
    }

    @Override
    public void addButtons(Panel panel) {
        panel.add(new ButtonCreateTeam(panel, canCreate));

        for (PublicTeamData t : teams) {
            panel.add(new ButtonTeam(panel, t));
        }
    }
}
