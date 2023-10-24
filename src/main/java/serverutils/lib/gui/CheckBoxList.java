package serverutils.lib.gui;

import java.util.ArrayList;
import java.util.List;

import serverutils.lib.client.GlStateManager;
import serverutils.lib.util.misc.MouseButton;

public class CheckBoxList extends Button {

    public static class CheckBoxEntry {

        public String name;
        public int value = 0;
        private CheckBoxList checkBoxList;

        public CheckBoxEntry(String n) {
            name = n;
        }

        public void onClicked(MouseButton button, int index) {
            select((value + 1) % checkBoxList.getValueCount());
            GuiHelper.playClickSound();
        }

        public void addMouseOverText(List<String> list) {}

        public CheckBoxEntry select(int v) {
            if (checkBoxList.radioButtons) {
                if (v > 0) {
                    for (CheckBoxEntry entry : checkBoxList.entries) {
                        boolean old1 = entry.value > 0;
                        entry.value = 0;

                        if (old1) {
                            entry.onValueChanged();
                        }
                    }
                } else {
                    return this;
                }
            }

            int old = value;
            value = v;

            if (old != value) {
                onValueChanged();
            }

            return this;
        }

        public void onValueChanged() {}
    }

    public final boolean radioButtons;
    private final List<CheckBoxEntry> entries;

    public CheckBoxList(GuiBase gui, boolean radiobutton) {
        super(gui);
        setSize(10, 2);
        radioButtons = radiobutton;
        entries = new ArrayList<>();
    }

    public int getValueCount() {
        return 2;
    }

    @Override
    public void drawBackground(Theme theme, int x, int y, int w, int h) {}

    public void drawCheckboxBackground(Theme theme, int x, int y, int w, int h) {
        theme.drawCheckboxBackground(x, y, w, h, radioButtons);
    }

    public void getCheckboxIcon(Theme theme, int x, int y, int w, int h, int index, int value) {
        theme.drawCheckbox(x, y, w, h, WidgetType.mouseOver(isMouseOver()), value != 0, radioButtons);
    }

    public void addBox(CheckBoxEntry checkBox) {
        checkBox.checkBoxList = this;
        entries.add(checkBox);
        setWidth(Math.max(width, getGui().getTheme().getStringWidth(checkBox.name)));
        setHeight(height + 11);
    }

    public CheckBoxEntry addBox(String name) {
        CheckBoxEntry entry = new CheckBoxEntry(name);
        addBox(entry);
        return entry;
    }

    @Override
    public void onClicked(MouseButton button) {
        int y = getMouseY() - getY();

        if (y % 11 == 10) {
            return;
        }

        int i = y / 11;

        if (i >= 0 && i < entries.size()) {
            entries.get(i).onClicked(button, i);
        }
    }

    @Override
    public void addMouseOverText(List<String> list) {}

    @Override
    public void draw(Theme theme, int x, int y, int w, int h) {
        drawBackground(theme, x, y, w, h);

        for (int i = 0; i < entries.size(); i++) {
            CheckBoxEntry entry = entries.get(i);
            int ey = y + i * 11 + 1;
            drawCheckboxBackground(theme, x, ey, 10, 10);
            getCheckboxIcon(theme, x + 1, ey + 1, 8, 8, i, entry.value);
            theme.drawString(entry.name, x + 12, ey + 1);
            GlStateManager.color(1F, 1F, 1F, 1F);
        }
    }
}
