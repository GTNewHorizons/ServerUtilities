package serverutils.client.gui.teams.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
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

    private class ButtonDimension extends SimpleTextButton {

        private final int dim;
        private final List<MessageAdminTeamClaimsList.Entry> dimEntries;

        private ButtonDimension(Panel panel, int dim, List<MessageAdminTeamClaimsList.Entry> dimEntries) {
            super(
                    panel,
                    ServerUtils.getDimensionName(dim).getFormattedText() + " (" + dimEntries.size() + ")",
                    GuiIcons.GLOBE);
            this.dim = dim;
            this.dimEntries = dimEntries;
        }

        @Override
        public void onClicked(MouseButton button) {
            GuiHelper.playClickSound();
            new GuiAdminManageClaimsDim(teamId, dim, dimEntries).openGui();
        }
    }

    private final String teamId;
    private final List<MessageAdminTeamClaimsList.Entry> entries;
    private final Int2ObjectMap<List<MessageAdminTeamClaimsList.Entry>> entriesByDim;

    public GuiAdminManageClaims(String teamId, Collection<MessageAdminTeamClaimsList.Entry> e) {
        this.teamId = teamId;
        setTitle(I18n.format("serverutilities.admin_panel.claimed_chunks"));
        setHasSearchBox(true);
        entries = new ArrayList<>(e);
        entries.sort(
                (a, b) -> a.dim == b.dim ? a.x == b.x ? Integer.compare(a.z, b.z) : Integer.compare(a.x, b.x)
                        : Integer.compare(a.dim, b.dim));

        entriesByDim = new Int2ObjectAVLTreeMap<>();
        for (MessageAdminTeamClaimsList.Entry entry : entries) {
            entriesByDim.computeIfAbsent(entry.dim, k -> new ArrayList<>()).add(entry);
        }
    }

    @Override
    public void addButtons(Panel panel) {
        panel.add(new ButtonUnclaimAll(panel));

        for (Map.Entry<Integer, List<MessageAdminTeamClaimsList.Entry>> group : entriesByDim.int2ObjectEntrySet()) {
            panel.add(new ButtonDimension(panel, group.getKey(), group.getValue()));
        }
    }
}
