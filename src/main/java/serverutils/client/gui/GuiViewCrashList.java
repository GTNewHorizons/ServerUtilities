package serverutils.client.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.client.resources.I18n;

import serverutils.lib.gui.GuiHelper;
import serverutils.lib.gui.Panel;
import serverutils.lib.gui.SimpleTextButton;
import serverutils.lib.gui.misc.GuiButtonListBase;
import serverutils.lib.icon.Icon;
import serverutils.lib.util.StringUtils;
import serverutils.lib.util.misc.MouseButton;
import serverutils.net.MessageViewCrash;

public class GuiViewCrashList extends GuiButtonListBase {

    private static class ButtonFile extends SimpleTextButton {

        public ButtonFile(Panel panel, String title) {
            super(panel, title, Icon.EMPTY);
        }

        @Override
        public void onClicked(MouseButton button) {
            GuiHelper.playClickSound();
            new MessageViewCrash(getTitle()).sendToServer();
        }
    }

    private final List<String> files;

    public GuiViewCrashList(Collection<String> l) {
        files = new ArrayList<>(l);
        files.sort(StringUtils.IGNORE_CASE_COMPARATOR.reversed());
    }

    @Override
    public String getTitle() {
        return I18n.format("sidebar_button.serverutilities.admin_panel") + " > "
                + I18n.format("serverutilities.admin_panel.crash_reports");
    }

    @Override
    public void addButtons(Panel panel) {
        for (String s : files) {
            panel.add(new ButtonFile(panel, s));
        }
    }
}
