package serverutils.lib.gui;

public class PanelScrollBar extends ScrollBar {

    public final Panel panel;

    public PanelScrollBar(Panel parent, Plane plane, Panel p) {
        super(parent, plane, 0);
        panel = p;
        panel.attachedScrollbar = this;
    }

    public PanelScrollBar(Panel parent, Panel panel) {
        this(parent, Plane.VERTICAL, panel);
    }

    @Override
    public void setMinValue(int min) {}

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public void setMaxValue(int max) {
        super.setMaxValue(max - (plane.isVertical ? panel.height : panel.width));
    }

    @Override
    public void setScrollStep(int s) {
        panel.setScrollStep(s);
    }

    @Override
    public int getScrollStep() {
        return panel.getScrollStep();
    }

    @Override
    public int getScrollBarSize() {
        int max = getMaxValue();

        if (max <= 0) {
            return 0;
        }

        int size;

        if (plane.isVertical) {
            size = (int) (panel.height / (double) (max + panel.height) * height);
        } else {
            size = (int) (panel.width / (double) (max + panel.width) * width);
        }

        return size < 10 ? 10 : size;
    }

    @Override
    public void onMoved() {
        int value = getMaxValue() <= 0 ? 0 : getValue();

        if (plane.isVertical) {
            panel.setScrollY(value);
        } else {
            panel.setScrollX(value);
        }
    }

    @Override
    public boolean canMouseScroll() {
        return super.canMouseScroll() || panel.isMouseOver();
    }
}
