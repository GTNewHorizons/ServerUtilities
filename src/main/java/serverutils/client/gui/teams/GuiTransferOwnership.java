package serverutils.client.gui.teams;

import java.util.Collection;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;

import serverutils.lib.data.ServerUtilitiesTeamGuiActions;
import serverutils.lib.gui.GuiHelper;
import serverutils.lib.gui.Panel;
import serverutils.lib.util.misc.MouseButton;
import serverutils.net.MessageMyTeamAction;
import serverutils.net.MessageMyTeamPlayerList;

public class GuiTransferOwnership extends GuiManagePlayersBase {

    private static class ButtonPlayer extends ButtonPlayerBase {

        private ButtonPlayer(Panel panel, MessageMyTeamPlayerList.Entry m) {
            super(panel, m);
        }

        @Override
        public void addMouseOverText(List<String> list) {}

        @Override
        public void onClicked(MouseButton button) {
            GuiHelper.playClickSound();

            getGui().openYesNo(
                    I18n.format("team_action.serverutilities.transfer_ownership") + "?",
                    Minecraft.getMinecraft().getSession().getUsername() + " => " + entry.name,
                    () -> {
                        getGui().closeGui(false);
                        NBTTagCompound data = new NBTTagCompound();
                        data.setString("player", entry.name);
                        new MessageMyTeamAction(ServerUtilitiesTeamGuiActions.TRANSFER_OWNERSHIP.getId(), data)
                                .sendToServer();
                    });
        }
    }

    public GuiTransferOwnership(Collection<MessageMyTeamPlayerList.Entry> m) {
        super(I18n.format("team_action.serverutilities.transfer_ownership"), m, ButtonPlayer::new);
    }
}
