package serverutils.client.gui.teams.admin;

import java.util.List;

import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;

import serverutils.lib.gui.GuiHelper;
import serverutils.lib.gui.GuiIcons;
import serverutils.lib.gui.Panel;
import serverutils.lib.gui.SimpleTextButton;
import serverutils.lib.gui.WidgetType;
import serverutils.lib.gui.misc.GuiButtonListBase;
import serverutils.lib.util.ServerUtils;
import serverutils.lib.util.misc.MouseButton;
import serverutils.net.MessageAdminTeamAction;
import serverutils.net.MessageAdminTeamClaimsList;

public class GuiAdminManageClaimsDim extends GuiButtonListBase {

    private class ButtonUnclaimAll extends SimpleTextButton {

        private ButtonUnclaimAll(Panel panel) {
            super(panel, I18n.format("serverutilities.lang.chunks.unclaim_all"), GuiIcons.BIN);
        }

        @Override
        public void onClicked(MouseButton button) {
            GuiHelper.playClickSound();

            getGui().openYesNo(
                    I18n.format("serverutilities.lang.chunks.unclaim_all_q"),
                    entries.size() + " chunks",
                    () -> {
                        NBTTagCompound data = new NBTTagCompound();
                        data.setBoolean("all", true);
                        data.setInteger("dim", dim);
                        new MessageAdminTeamAction(teamId, MessageAdminTeamAction.CLAIMS, data).sendToServer();
                        getGui().closeGui(true);
                    });
        }

        @Override
        public WidgetType getWidgetType() {
            return entries.isEmpty() ? WidgetType.DISABLED : WidgetType.mouseOver(isMouseOver());
        }
    }

    private class ButtonClaim extends SimpleTextButton {

        private final MessageAdminTeamClaimsList.Entry entry;

        private ButtonClaim(Panel panel, MessageAdminTeamClaimsList.Entry e) {
            super(panel, e.x + ", " + e.z, e.loaded ? GuiIcons.BEACON : GuiIcons.MAP);
            entry = e;
        }

        @Override
        public void addMouseOverText(List<String> list) {
            if (entry.loaded) {
                list.add(EnumChatFormatting.GREEN + I18n.format("serverutilities.lang.chunks.chunk_loaded"));
            }

            list.add(I18n.format("serverutilities.admin_panel.claims.click_to_unclaim"));
            list.add(I18n.format("serverutilities.admin_panel.claims.shift_click_to_teleport"));
            list.add(I18n.format("serverutilities.admin_panel.claims.ctrl_click_to_toggle_load"));
        }

        @Override
        public void onClicked(MouseButton button) {
            GuiHelper.playClickSound();

            if (isShiftKeyDown()) {
                NBTTagCompound data = newActionData();
                data.setBoolean("teleport", true);
                new MessageAdminTeamAction(teamId, MessageAdminTeamAction.CLAIMS, data).sendToServer();
                return;
            }

            if (isCtrlKeyDown()) {
                entry.loaded = !entry.loaded;
                setIcon(entry.loaded ? GuiIcons.BEACON : GuiIcons.MAP);

                NBTTagCompound data = newActionData();
                data.setBoolean("load", entry.loaded);
                new MessageAdminTeamAction(teamId, MessageAdminTeamAction.CLAIMS, data).sendToServer();
                return;
            }

            getGui().openYesNo(I18n.format("serverutilities.admin_panel.claims.unclaim_q"), getTitle(), () -> {
                new MessageAdminTeamAction(teamId, MessageAdminTeamAction.CLAIMS, newActionData()).sendToServer();
                getGui().closeGui(true);
            });
        }

        private NBTTagCompound newActionData() {
            NBTTagCompound data = new NBTTagCompound();
            data.setInteger("dim", entry.dim);
            data.setInteger("x", entry.x);
            data.setInteger("z", entry.z);
            return data;
        }
    }

    private final String teamId;
    private final int dim;
    private final List<MessageAdminTeamClaimsList.Entry> entries;

    public GuiAdminManageClaimsDim(String teamId, int dim, List<MessageAdminTeamClaimsList.Entry> entries) {
        this.teamId = teamId;
        this.dim = dim;
        this.entries = entries;
        setTitle(ServerUtils.getDimensionName(dim).getFormattedText());
        setHasSearchBox(true);
    }

    @Override
    public void addButtons(Panel panel) {
        panel.add(new ButtonUnclaimAll(panel));

        for (MessageAdminTeamClaimsList.Entry entry : entries) {
            panel.add(new ButtonClaim(panel, entry));
        }
    }
}
