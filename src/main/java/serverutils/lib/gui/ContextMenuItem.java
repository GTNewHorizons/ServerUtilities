package serverutils.lib.gui;

import java.util.List;
import java.util.function.BooleanSupplier;

import javax.annotation.Nullable;

import serverutils.lib.icon.Icon;
import serverutils.lib.util.StringUtils;
import serverutils.lib.util.misc.MouseButton;

public class ContextMenuItem implements Comparable<ContextMenuItem> {

    public static final ContextMenuItem SEPARATOR = new ContextMenuItem("", Icon.EMPTY, () -> {}) {

        @Override
        public Widget createWidget(ContextMenu panel) {
            return new ContextMenu.CSeperator(panel);
        }
    };

    public static final BooleanSupplier TRUE = () -> true;
    public static final BooleanSupplier FALSE = () -> false;

    public String title;
    public Icon icon;
    public Runnable callback;
    public BooleanSupplier enabled = TRUE;
    public String yesNoText = "";
    public boolean closeMenu = true;

    public ContextMenuItem(String t, Icon i, @Nullable Runnable c) {
        title = t;
        icon = i;
        callback = c;
    }

    public void addMouseOverText(List<String> list) {}

    public void drawIcon(Theme theme, int x, int y, int w, int h) {
        icon.draw(x, y, w, h);
    }

    public ContextMenuItem setEnabled(boolean v) {
        return setEnabled(v ? TRUE : FALSE);
    }

    public ContextMenuItem setEnabled(BooleanSupplier v) {
        enabled = v;
        return this;
    }

    public ContextMenuItem setYesNo(String s) {
        yesNoText = s;
        return this;
    }

    public ContextMenuItem setCloseMenu(boolean v) {
        closeMenu = v;
        return this;
    }

    public Widget createWidget(ContextMenu panel) {
        return new ContextMenu.CButton(panel, this);
    }

    @Override
    public int compareTo(ContextMenuItem o) {
        return StringUtils.unformatted(title).compareToIgnoreCase(StringUtils.unformatted(o.title));
    }

    public void onClicked(Panel panel, MouseButton button) {
        if (closeMenu) {
            panel.getGui().closeContextMenu();
        }

        callback.run();
    }
}
