package serverutils.client.gui.teams.admin;

import java.util.Collection;

import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;

import serverutils.client.gui.teams.GuiManagePlayersBase;
import serverutils.lib.gui.GuiHelper;
import serverutils.lib.gui.Panel;
import serverutils.lib.util.misc.MouseButton;
import serverutils.net.MessageAdminTeamAction;
import serverutils.net.MessageMyTeamPlayerList;

public class GuiAdminSetOwner extends GuiManagePlayersBase {

    private static class ButtonPlayer extends ButtonPlayerBase {

        private final String teamId;

        private ButtonPlayer(Panel panel, String teamId, MessageMyTeamPlayerList.Entry m) {
            super(panel, m);
            this.teamId = teamId;
        }

        @Override
        public void onClicked(MouseButton button) {
            GuiHelper.playClickSound();

            getGui().openYesNo(I18n.format("team_action.serverutilities.transfer_ownership") + "?", entry.name, () -> {
                getGui().closeGui(false);
                NBTTagCompound data = new NBTTagCompound();
                data.setString("player", entry.name);
                new MessageAdminTeamAction(teamId, MessageAdminTeamAction.OWNER, data).sendToServer();
            });
        }
    }

    public GuiAdminSetOwner(String teamId, Collection<MessageMyTeamPlayerList.Entry> m) {
        super(
                I18n.format("team_action.serverutilities.transfer_ownership"),
                m,
                (panel, e) -> new ButtonPlayer(panel, teamId, e));
    }
}
