package serverutils.lib.gui;

import javax.annotation.Nullable;

import serverutils.lib.icon.Color4I;

public class ColorWidget extends Widget {

    public final Color4I color;
    public Color4I mouseOverColor;

    public ColorWidget(Panel panel, Color4I c, @Nullable Color4I m) {
        super(panel);
        color = c;
        mouseOverColor = m;
    }

    @Override
    public void draw(Theme theme, int x, int y, int w, int h) {
        ((mouseOverColor != null && isMouseOver()) ? mouseOverColor : color).draw(x, y, w, h);
    }
}
