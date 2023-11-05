package serverutils.lib.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;

import serverutils.ServerUtilities;
import serverutils.lib.icon.Color4I;
import serverutils.lib.util.misc.MouseButton;

public abstract class Panel extends Widget {

    public final List<Widget> widgets;
    private double scrollX = 0, scrollY = 0;
    private int offsetX = 0, offsetY = 0;
    private boolean unicode = false;
    private boolean onlyRenderWidgetsInside = true;
    private boolean onlyInteractWithWidgetsInside = true;
    private int scrollStep = 20;
    private int contentWidth = -1, contentHeight = -1;
    public int contentWidthExtra, contentHeightExtra;
    public PanelScrollBar attachedScrollbar = null;

    public Panel(Panel panel) {
        super(panel);
        widgets = new ArrayList<>();
    }

    public boolean getUnicode() {
        return unicode;
    }

    public void setUnicode(boolean value) {
        unicode = value;
    }

    public boolean getOnlyRenderWidgetsInside() {
        return onlyRenderWidgetsInside;
    }

    public void setOnlyRenderWidgetsInside(boolean value) {
        onlyRenderWidgetsInside = value;
    }

    public boolean getOnlyInteractWithWidgetsInside() {
        return onlyInteractWithWidgetsInside;
    }

    public void setOnlyInteractWithWidgetsInside(boolean value) {
        onlyInteractWithWidgetsInside = value;
    }

    public abstract void addWidgets();

    public abstract void alignWidgets();

    public void clearWidgets() {
        widgets.clear();
    }

    public void refreshWidgets() {
        contentWidth = contentHeight = -1;
        clearWidgets();
        Theme theme = getGui().getTheme();
        theme.pushFontUnicode(getUnicode());

        try {
            addWidgets();
        } catch (MismatchingParentPanelException ex) {
            ServerUtilities.LOGGER.error(ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // alignWidgets();

        for (Widget widget : widgets) {
            if (widget instanceof Panel) {
                ((Panel) widget).refreshWidgets();
            }
        }

        alignWidgets();
        theme.popFontUnicode();
    }

    public void add(Widget widget) {
        if (widget.parent != this) {
            throw new MismatchingParentPanelException(this, widget);
        }

        widgets.add(widget);
        contentWidth = contentHeight = -1;
    }

    public void addAll(Iterable<? extends Widget> list) {
        for (Widget w : list) {
            add(w);
        }
    }

    public final int align(WidgetLayout layout) {
        return layout.align(this);
    }

    @Override
    public int getX() {
        return super.getX() + offsetX;
    }

    @Override
    public int getY() {
        return super.getY() + offsetY;
    }

    public int getContentWidth() {
        if (contentWidth == -1) {
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;

            for (Widget widget : widgets) {
                if (widget.posX < minX) {
                    minX = widget.posX;
                }

                if (widget.posX + widget.width > maxX) {
                    maxX = widget.posX + widget.width;
                }
            }

            contentWidth = maxX - minX + contentWidthExtra;
        }

        return contentWidth;
    }

    public int getContentHeight() {
        if (contentHeight == -1) {
            int minY = Integer.MAX_VALUE;
            int maxY = Integer.MIN_VALUE;

            for (Widget widget : widgets) {
                if (widget.posY < minY) {
                    minY = widget.posY;
                }

                if (widget.posY + widget.height > maxY) {
                    maxY = widget.posY + widget.height;
                }
            }

            contentHeight = maxY - minY + contentHeightExtra;
        }

        return contentHeight;
    }

    public void setOffset(boolean flag) {
        if (flag) {
            offsetX = (int) -scrollX;
            offsetY = (int) -scrollY;
        } else {
            offsetX = offsetY = 0;
        }
    }

    public boolean isOffset() {
        return offsetX != 0 || offsetY != 0;
    }

    public void setScrollX(double scroll) {
        scrollX = scroll;
    }

    public void setScrollY(double scroll) {
        scrollY = scroll;
    }

    public double getScrollX() {
        return scrollX;
    }

    public double getScrollY() {
        return scrollY;
    }

    @Override
    public void draw(Theme theme, int x, int y, int w, int h) {
        boolean renderInside = getOnlyRenderWidgetsInside();
        theme.pushFontUnicode(getUnicode());

        drawBackground(theme, x, y, w, h);

        if (renderInside) {
            GuiHelper.pushScissor(getScreen(), x, y, w, h);
        }

        setOffset(true);
        drawOffsetBackground(theme, x + offsetX, y + offsetY, w, h);

        for (int i = 0; i < widgets.size(); i++) {
            Widget widget = widgets.get(i);

            if (widget.shouldDraw() && (!renderInside || widget.collidesWith(x, y, w, h))) {
                drawWidget(theme, widget, i, x + offsetX, y + offsetY, w, h);
            }
        }

        setOffset(false);

        if (renderInside) {
            GuiHelper.popScissor(getScreen());
        }

        theme.popFontUnicode();
    }

    public void drawBackground(Theme theme, int x, int y, int w, int h) {}

    public void drawOffsetBackground(Theme theme, int x, int y, int w, int h) {}

    public void drawWidget(Theme theme, Widget widget, int index, int x, int y, int w, int h) {
        int wx = widget.getX();
        int wy = widget.getY();
        int ww = widget.width;
        int wh = widget.height;

        widget.draw(theme, wx, wy, ww, wh);

        if (Theme.renderDebugBoxes) {
            Color4I col = Color4I.rgb(java.awt.Color.HSBtoRGB((widget.hashCode() & 255) / 255F, 1F, 1F));
            GuiHelper.drawHollowRect(wx, wy, ww, wh, col.withAlpha(150), false);
            col.withAlpha(30).draw(wx + 1, wy + 1, ww - 2, wh - 2);
        }
    }

    @Override
    public void addMouseOverText(List<String> list) {
        if (!shouldAddMouseOverText() || getOnlyInteractWithWidgetsInside() && !isMouseOver()) {
            return;
        }

        Theme theme = getGui().getTheme();
        theme.pushFontUnicode(getUnicode());
        setOffset(true);

        for (int i = widgets.size() - 1; i >= 0; i--) {
            Widget widget = widgets.get(i);

            if (widget.shouldAddMouseOverText()) {
                widget.addMouseOverText(list);

                if (Theme.renderDebugBoxes) {
                    list.add(
                            EnumChatFormatting.DARK_GRAY + widget
                                    .toString() + "#" + (i + 1) + ": " + widget.width + "x" + widget.height);
                }
            }
        }

        setOffset(false);
        theme.popFontUnicode();
    }

    @Override
    public void updateMouseOver(int mouseX, int mouseY) {
        super.updateMouseOver(mouseX, mouseY);
        setOffset(true);

        for (Widget widget : widgets) {
            widget.updateMouseOver(mouseX, mouseY);
        }

        setOffset(false);
    }

    @Override
    public boolean mousePressed(MouseButton button) {
        if (getOnlyInteractWithWidgetsInside() && !isMouseOver()) {
            return false;
        }

        setOffset(true);

        for (int i = widgets.size() - 1; i >= 0; i--) {
            Widget widget = widgets.get(i);

            if (widget.isEnabled()) {
                if (widget.mousePressed(button)) {
                    setOffset(false);
                    return true;
                }
            }
        }

        setOffset(false);
        return false;
    }

    @Override
    public void mouseReleased(MouseButton button) {
        setOffset(true);

        for (int i = widgets.size() - 1; i >= 0; i--) {
            Widget widget = widgets.get(i);

            if (widget.isEnabled()) {
                widget.mouseReleased(button);
            }
        }

        setOffset(false);
    }

    @Override
    public boolean mouseScrolled(int scroll) {
        setOffset(true);

        for (int i = widgets.size() - 1; i >= 0; i--) {
            Widget widget = widgets.get(i);

            if (widget.isEnabled()) {
                if (widget.mouseScrolled(scroll)) {
                    setOffset(false);
                    return true;
                }
            }
        }

        boolean scrollPanel = scrollPanel(scroll);
        setOffset(false);
        return scrollPanel;
    }

    public boolean scrollPanel(int scroll) {
        if (attachedScrollbar != null || !isMouseOver()) {
            return false;
        }

        if (isDefaultScrollVertical() != isShiftKeyDown()) {
            return movePanelScroll(0, -getScrollStep() * scroll);
        } else {
            return movePanelScroll(-getScrollStep() * scroll, 0);
        }
    }

    public boolean movePanelScroll(double dx, double dy) {
        if (dx == 0 && dy == 0) {
            return false;
        }

        double sx = getScrollX();
        double sy = getScrollY();

        if (dx != 0) {
            int w = getContentWidth();

            if (w > width) {
                setScrollX(MathHelper.clamp_double(sx + dx, 0, w - width));
            }
        }

        if (dy != 0) {
            int h = getContentHeight();

            if (h > height) {
                setScrollY(MathHelper.clamp_double(sy + dy, 0, h - height));
            }
        }

        return getScrollX() != sx || getScrollY() != sy;
    }

    public boolean isDefaultScrollVertical() {
        return true;
    }

    public void setScrollStep(int s) {
        scrollStep = s;
    }

    public int getScrollStep() {
        return scrollStep;
    }

    @Override
    public boolean keyPressed(int key, char keyChar) {
        if (super.keyPressed(key, keyChar)) {
            return true;
        }

        setOffset(true);

        for (int i = widgets.size() - 1; i >= 0; i--) {
            Widget widget = widgets.get(i);

            if (widget.isEnabled() && widget.keyPressed(key, keyChar)) {
                setOffset(false);
                return true;
            }
        }

        setOffset(false);
        return false;
    }

    @Override
    public void keyReleased(int key) {
        setOffset(true);

        for (int i = widgets.size() - 1; i >= 0; i--) {
            Widget widget = widgets.get(i);

            if (widget.isEnabled()) {
                widget.keyReleased(key);
            }
        }

        setOffset(false);
    }

    @Override
    public void onClosed() {
        for (Widget widget : widgets) {
            widget.onClosed();
        }
    }

    @Nullable
    public Widget getWidget(int index) {
        return index < 0 || index >= widgets.size() ? null : widgets.get(index);
    }

    @Override
    @Nullable
    public Object getIngredientUnderMouse() {
        setOffset(true);

        for (int i = widgets.size() - 1; i >= 0; i--) {
            Widget widget = widgets.get(i);

            if (widget.isEnabled() && widget.isMouseOver()) {
                Object object = widget.getIngredientUnderMouse();

                if (object != null) {
                    setOffset(false);
                    return object;
                }
            }
        }

        setOffset(false);
        return null;
    }

    @Override
    public void tick() {
        setOffset(true);

        for (Widget widget : widgets) {
            if (widget.isEnabled()) {
                widget.tick();
            }
        }

        setOffset(false);
    }

    public boolean isMouseOverAnyWidget() {
        for (Widget widget : widgets) {
            if (widget.isMouseOver()) {
                return true;
            }
        }

        return false;
    }
}
