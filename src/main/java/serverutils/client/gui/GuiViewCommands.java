package serverutils.client.gui;

import java.util.List;
import java.util.Map;

import net.minecraft.util.IChatComponent;

import serverutils.lib.client.ClientUtils;
import serverutils.lib.gui.GuiHelper;
import serverutils.lib.gui.Panel;
import serverutils.lib.gui.SimpleTextButton;
import serverutils.lib.gui.misc.GuiButtonListBase;
import serverutils.lib.icon.Icon;
import serverutils.lib.util.misc.MouseButton;

public class GuiViewCommands extends GuiButtonListBase {

    private final Map<String, IChatComponent> commands;

    public GuiViewCommands(Map<String, IChatComponent> commands) {
        this.commands = commands;
        setHasSearchBox(true);
    }

    @Override
    public void addButtons(Panel panel) {
        commands.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEachOrdered(entry -> panel.add(new CommandEntry(panel, entry.getKey(), entry.getValue())));
    }

    public static class CommandEntry extends SimpleTextButton {

        private final IChatComponent description;

        public CommandEntry(Panel panel, String title, IChatComponent description) {
            super(panel, title, Icon.EMPTY);
            this.description = description;
            setSize(16, 14);
        }

        @Override
        public void onClicked(MouseButton button) {
            GuiHelper.playClickSound();
            ClientUtils.execClientCommand("/" + getTitle());
        }

        @Override
        public void addMouseOverText(List<String> list) {
            list.addAll(getGui().getTheme().listFormattedStringToWidth(description.getFormattedText(), 200));
        }

        @Override
        public boolean renderTitleInCenter() {
            return true;
        }
    }
}
