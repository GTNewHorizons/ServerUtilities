package serverutils.client.gui.teams.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;

import serverutils.client.gui.teams.PublicTeamData;
import serverutils.lib.gui.GuiHelper;
import serverutils.lib.gui.Panel;
import serverutils.lib.gui.SimpleTextButton;
import serverutils.lib.gui.misc.GuiButtonListBase;
import serverutils.lib.util.misc.MouseButton;

public class GuiAdminSelectTeam extends GuiButtonListBase {

    private static class ButtonTeam extends SimpleTextButton {

        private final PublicTeamData team;

        private ButtonTeam(Panel panel, PublicTeamData t) {
            super(panel, t.displayName.getUnformattedText(), t.icon.withBorder(t.color.getColor(), false));
            team = t;
        }

        @Override
        public void onClicked(MouseButton button) {
            GuiHelper.playClickSound();
            new GuiAdminEditTeam(team.getId(), team.displayName.getFormattedText()).openGui();
        }

        @Override
        public void addMouseOverText(List<String> list) {
            if (!team.description.isEmpty()) {
                list.add(EnumChatFormatting.ITALIC + team.description);
            }
        }
    }

    private final List<PublicTeamData> teams;

    public GuiAdminSelectTeam(Collection<PublicTeamData> teams) {
        setTitle(I18n.format("serverutilities.admin_panel.edit_team"));
        setHasSearchBox(true);
        this.teams = new ArrayList<>(teams);
        this.teams.sort(null);
    }

    @Override
    public void addButtons(Panel panel) {
        for (PublicTeamData team : teams) {
            panel.add(new ButtonTeam(panel, team));
        }
    }
}
