package serverutils.client.gui.ranks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;

import serverutils.lib.config.ConfigValueInstance;
import serverutils.lib.gui.Button;
import serverutils.lib.gui.ContextMenuItem;
import serverutils.lib.gui.GuiIcons;
import serverutils.lib.gui.Panel;
import serverutils.lib.gui.SimpleButton;
import serverutils.lib.gui.Widget;
import serverutils.lib.gui.misc.GuiEditConfig;
import serverutils.lib.icon.Color4I;
import serverutils.lib.util.misc.MouseButton;
import serverutils.net.MessageRankModify;

public class GuiEditRank extends GuiEditConfig {

    private final Button buttonAddPermission;
    private final Button buttonAddCommand;
    private final RankInst rank;

    public GuiEditRank(RankInst inst) {
        super(inst.group, null);
        rank = inst;
        buttonAddPermission = new SimpleButton(
                this,
                StatCollector.translateToLocal("serverutilities.admin_panel.ranks.add_perm"),
                GuiIcons.ADD,
                (widget, button) -> openContextMenu(new GuiAddPermission(group, GuiRanks.allPerms)));
        buttonAddCommand = new SimpleButton(
                this,
                StatCollector.translateToLocal("serverutilities.admin_panel.ranks.add_command"),
                GuiIcons.ADD_GRAY.withColor(Color4I.YELLOW),
                (widget, button) -> openContextMenu(new GuiAddPermission(group, GuiRanks.commandPerms)));
    }

    @Override
    public void addWidgets() {
        super.addWidgets();
        add(buttonAddPermission);
        add(buttonAddCommand);
    }

    @Override
    public void alignWidgets() {
        super.alignWidgets();
        buttonAddPermission.setPos(width - 58, 2);
        buttonAddCommand.setPos(width - 78, 2);
    }

    @Override
    public void onClosed() {
        if (shouldClose == 1) {
            List<String> removedEntries = new ArrayList<>();
            for (ConfigValueInstance inst : originalGroup.getValues()) {
                if (rank.group.getValueInstance(inst.getId()) == null) {
                    removedEntries.add(inst.getId());
                }
            }
            new MessageRankModify(rank, removedEntries).sendToServer();
        }
        super.onClosed();
    }

    @Override
    public boolean onClosedByKey(int key) {
        if (key == Keyboard.KEY_ESCAPE || Minecraft.getMinecraft().gameSettings.keyBindInventory.getKeyCode() == key) {
            openUnsavedYesNo(save -> sendAndExit(save, false));
        }

        return false;
    }

    @Override
    public void onBack() {
        openUnsavedYesNo(save -> sendAndExit(save, true));
    }

    private void sendAndExit(boolean save, boolean back) {
        if (save) {
            shouldClose = 1;
        }

        closeGui(back);
    }

    public void removeEntry(String value) {
        Predicate<Widget> predicate = widget -> widget instanceof ButtonNodeEntry btn && btn.inst.getId().equals(value);
        configEntryButtons.removeIf(predicate);
        configPanel.widgets.removeIf(predicate);
        group.removeValue(value);
        alignWidgets();
    }

    public void addEntry(String value) {
        ConfigValueInstance inst = GuiRanks.getValue(value);
        if (inst == null) return;
        group.add(inst.copy(group));
        configPanel.add(getEntryButton(configPanel, null, inst));
        configEntryButtons.add(getEntryButton(configPanel, null, inst));
        configPanel.refreshWidgets();
        alignWidgets();
    }

    @Override
    protected Widget getEntryButton(Panel panel, @Nullable ButtonConfigGroup group, ConfigValueInstance instance) {
        groupSize = 1;
        return new ButtonNodeEntry(panel, instance);
    }

    protected class ButtonNodeEntry extends ButtonConfigEntry {

        private final List<ContextMenuItem> contextItems;

        public ButtonNodeEntry(Panel panel, ConfigValueInstance i) {
            super(panel, null, i);
            contextItems = new ArrayList<>();
            ContextMenuItem item = new ContextMenuItem(
                    StatCollector.translateToLocal("selectServer.delete"),
                    GuiIcons.REMOVE,
                    () -> removeEntry(i.getId()));
            contextItems.add(item);
        }

        @Override
        protected void addDescriptionText(List<String> list) {
            super.addDescriptionText(list);
            if (StatCollector.canTranslate("permission." + inst.getId())) {
                list.add(StatCollector.translateToLocal("permission." + inst.getId()));
            }
        }

        @Override
        public void onClicked(MouseButton button) {
            if (button.isLeft()) {
                super.onClicked(button);
            }

            if (button == MouseButton.RIGHT) {
                GuiEditRank.this.openContextMenu(contextItems);
            }
        }
    }
}
