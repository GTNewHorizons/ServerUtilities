package serverutils.lib.gui;

import java.util.List;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;

import serverutils.lib.math.MathUtils;
import serverutils.lib.util.misc.MouseButton;

public class ScrollBar extends Widget {

    public enum Plane {

        HORIZONTAL(false),
        VERTICAL(true);

        public final boolean isVertical;

        Plane(boolean v) {
            isVertical = v;
        }
    }

    public final Plane plane;
    private int scrollBarSize;
    private int value = 0;
    private int scrollStep = 20;
    private int grab = -10000;
    private int minValue = 0;
    private int maxValue = 100;
    private boolean canAlwaysScroll = false;
    private boolean canAlwaysScrollPlane = true;

    public ScrollBar(Panel parent, Plane p, int ss) {
        super(parent);
        plane = p;
        scrollBarSize = Math.max(ss, 0);
    }

    public void setCanAlwaysScroll(boolean v) {
        canAlwaysScroll = v;
    }

    public void setCanAlwaysScrollPlane(boolean v) {
        canAlwaysScrollPlane = v;
    }

    public void setMinValue(int min) {
        minValue = min;
        setValue(getValue());
    }

    public int getMinValue() {
        return minValue;
    }

    public void setMaxValue(int max) {
        maxValue = max;
        setValue(getValue());
    }

    public int getMaxValue() {
        return maxValue;
    }

    public void setScrollStep(int s) {
        scrollStep = Math.max(1, s);
    }

    public int getScrollBarSize() {
        return scrollBarSize;
    }

    @Override
    public boolean mousePressed(MouseButton button) {
        if (isMouseOver()) {
            grab = (plane.isVertical ? (getMouseY() - (getY() + getValueI(height - getScrollBarSize())))
                    : (getMouseX() - (getX() + getValueI(width - getScrollBarSize()))));
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(int scroll) {
        if (scroll != 0 && canMouseScrollPlane() && canMouseScroll()) {
            setValue(getValue() - getScrollStep() * scroll);
            return true;
        }

        return false;
    }

    @Override
    public void addMouseOverText(List<String> list) {
        if (showValueOnMouseOver()) {
            String t = getTitle();
            list.add(t.isEmpty() ? Integer.toString(getValue()) : (t + ": " + getValue()));
        }

        if (Theme.renderDebugBoxes) {
            list.add(EnumChatFormatting.DARK_GRAY + "Size: " + getScrollBarSize());
            list.add(EnumChatFormatting.DARK_GRAY + "Max: " + getMaxValue());
            list.add(EnumChatFormatting.DARK_GRAY + "Value: " + getValue());
        }
    }

    public boolean showValueOnMouseOver() {
        return false;
    }

    @Override
    public void draw(Theme theme, int x, int y, int w, int h) {
        int scrollBarSize = getScrollBarSize();

        if (scrollBarSize > 0) {
            int v = getValue();

            if (grab != -10000) {
                if (isMouseButtonDown(MouseButton.LEFT)) {
                    if (plane.isVertical) {
                        v = (int) ((getMouseY() - (y + grab)) * getMaxValue() / (double) (height - scrollBarSize));
                    } else {
                        v = (int) ((getMouseX() - (x + grab)) * getMaxValue() / (double) (width - scrollBarSize));
                    }
                } else {
                    grab = -10000;
                }
            }

            setValue(v);
        }

        drawBackground(theme, x, y, width, height);

        if (scrollBarSize > 0) {
            if (plane.isVertical) {
                drawScrollBar(theme, x, y + getValueI(height - scrollBarSize), width, scrollBarSize);
            } else {
                drawScrollBar(theme, x + getValueI(width - scrollBarSize), y, scrollBarSize, height);
            }
        }
    }

    public void drawBackground(Theme theme, int x, int y, int w, int h) {
        theme.drawScrollBarBackground(x, y, w, h, getWidgetType());
    }

    public void drawScrollBar(Theme theme, int x, int y, int w, int h) {
        theme.drawScrollBar(x, y, w, h, WidgetType.mouseOver(grab != -10000), plane.isVertical);
    }

    public void onMoved() {}

    public boolean canMouseScrollPlane() {
        return canAlwaysScrollPlane || isShiftKeyDown() != plane.isVertical;
    }

    public boolean canMouseScroll() {
        return canAlwaysScroll || isMouseOver();
    }

    public void setValue(int v) {
        v = MathHelper.clamp_int(v, getMinValue(), getMaxValue());

        if (value != v) {
            value = v;
            onMoved();
        }
    }

    public int getValue() {
        return value;
    }

    public int getValueI(int max) {
        return (int) MathUtils.map(getMinValue(), getMaxValue(), 0, max, value);
    }

    public int getScrollStep() {
        return scrollStep;
    }
}
