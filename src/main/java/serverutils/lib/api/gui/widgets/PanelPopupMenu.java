package serverutils.lib.api.gui.widgets;

import java.util.ArrayList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.api.gui.GuiLM;

@SideOnly(Side.CLIENT)
public abstract class PanelPopupMenu extends PanelLM {

    public int buttonHeight = 18;
    public final ArrayList<ButtonPopupMenu> menuButtons;

    public PanelPopupMenu(GuiLM g, int x, int y, int w) {
        super(g, x, y, w, 0);
        menuButtons = new ArrayList<>();
    }

    public final void addWidgets() {
        menuButtons.clear();
        addItems();

        width = 0;
        height = menuButtons.size() * (buttonHeight + 1) + 1;

        for (int i = 0; i < menuButtons.size(); i++) {
            ButtonPopupMenu b = menuButtons.get(i);
            b.posY = i * (buttonHeight + 1);
            add(b);
            width = Math.max(width, b.width);
        }

        for (ButtonPopupMenu b : menuButtons) b.width = width;
    }

    public abstract void addItems();

    public abstract void onClosed(ButtonPopupMenu b, int mb);

    public void renderWidget() {
        for (int i = 0; i < menuButtons.size(); i++) menuButtons.get(i).renderWidget();
    }

    public void mousePressed(int b) {
        if (mouseOver()) {
            for (int i = 0; i < menuButtons.size(); i++) menuButtons.get(i).mousePressed(b);
        } else onClosed(null, b);
    }
}
