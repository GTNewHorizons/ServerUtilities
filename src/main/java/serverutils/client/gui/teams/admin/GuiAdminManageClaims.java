package serverutils.client.gui.teams.admin;

import java.util.ArrayList;
import java.util.Collection;
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
import serverutils.lib.util.misc.MouseButton;
import serverutils.net.MessageAdminTeamAction;
import serverutils.net.MessageAdminTeamClaimsList;

public class GuiAdminManageClaims extends GuiButtonListBase {

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
            super(panel, "[" + e.dim + "] " + e.x + ", " + e.z, e.loaded ? GuiIcons.BEACON : GuiIcons.MAP);
            entry = e;
        }

        @Override
        public void addMouseOverText(List<String> list) {
            if (entry.loaded) {
                list.add(EnumChatFormatting.GREEN + I18n.format("serverutilities.lang.chunks.chunk_loaded"));
            }

            list.add(I18n.format("serverutilities.admin_panel.claims.click_to_unclaim"));
            list.add(I18n.format("serverutilities.admin_panel.claims.shift_click_to_teleport"));
        }

        @Override
        public void onClicked(MouseButton button) {
            GuiHelper.playClickSound();

            if (isShiftKeyDown()) {
                NBTTagCompound data = new NBTTagCompound();
                data.setBoolean("teleport", true);
                data.setInteger("dim", entry.dim);
                data.setInteger("x", entry.x);
                data.setInteger("z", entry.z);
                new MessageAdminTeamAction(teamId, MessageAdminTeamAction.CLAIMS, data).sendToServer();
                // getGui().closeGui(true);
                return;
            }

            getGui().openYesNo(I18n.format("serverutilities.admin_panel.claims.unclaim_q"), getTitle(), () -> {
                NBTTagCompound data = new NBTTagCompound();
                data.setInteger("dim", entry.dim);
                data.setInteger("x", entry.x);
                data.setInteger("z", entry.z);
                new MessageAdminTeamAction(teamId, MessageAdminTeamAction.CLAIMS, data).sendToServer();
                getGui().closeGui(true);
            });
        }
    }

    private final String teamId;
    private final List<MessageAdminTeamClaimsList.Entry> entries;

    public GuiAdminManageClaims(String teamId, Collection<MessageAdminTeamClaimsList.Entry> e) {
        this.teamId = teamId;
        setTitle(I18n.format("serverutilities.admin_panel.claimed_chunks"));
        setHasSearchBox(true);
        entries = new ArrayList<>(e);
        entries.sort(
                (a, b) -> a.dim == b.dim ? a.x == b.x ? Integer.compare(a.z, b.z) : Integer.compare(a.x, b.x)
                        : Integer.compare(a.dim, b.dim));
    }

    @Override
    public void addButtons(Panel panel) {
        panel.add(new ButtonUnclaimAll(panel));

        for (MessageAdminTeamClaimsList.Entry entry : entries) {
            panel.add(new ButtonClaim(panel, entry));
        }
    }
}
