package serverutils.lib.gui;

import javax.annotation.Nullable;

import serverutils.lib.icon.Icon;
import serverutils.lib.util.misc.MouseButton;

public abstract class Button extends Widget {

    protected String title = "";
    protected Icon icon = Icon.EMPTY;

    public Button(Panel panel) {
        super(panel);
        setSize(16, 16);
    }

    public Button(Panel panel, String t, Icon i) {
        this(panel);
        icon = i;
        title = t;
    }

    @Override
    public String getTitle() {
        return title;
    }

    public Button setTitle(String s) {
        title = s;
        return this;
    }

    public Button setIcon(Icon i) {
        icon = i;
        return this;
    }

    public void drawBackground(Theme theme, int x, int y, int w, int h) {
        theme.drawButton(x, y, w, h, getWidgetType());
    }

    public void drawIcon(Theme theme, int x, int y, int w, int h) {
        icon.draw(x, y, w, h);
    }

    @Override
    public void draw(Theme theme, int x, int y, int w, int h) {
        int s = h >= 16 ? 16 : 8;
        drawBackground(theme, x, y, w, h);
        drawIcon(theme, x + (w - s) / 2, y + (h - s) / 2, s, s);
    }

    @Override
    public boolean mousePressed(MouseButton button) {
        if (isMouseOver()) {
            if (getWidgetType() != WidgetType.DISABLED) {
                onClicked(button);
            }

            return true;
        }

        return false;
    }

    public abstract void onClicked(MouseButton button);

    @Override
    @Nullable
    public Object getIngredientUnderMouse() {
        return new WrappedIngredient(icon.getIngredient()).tooltip();
    }
}
