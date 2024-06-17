package serverutils.lib.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import serverutils.lib.client.GlStateManager;
import serverutils.lib.gui.misc.GuiButtonListBase;
import serverutils.lib.icon.Color4I;
import serverutils.lib.util.misc.MouseButton;

public class CheckBoxList extends Button {

    public static class CheckBoxEntry {

        public String name;
        public String displayName;
        public int value = 0;
        protected boolean locked = false;
        protected CheckBoxList checkBoxList;

        public CheckBoxEntry(String n) {
            name = n;
            displayName = n;
        }

        public void onClicked(MouseButton button, int index) {
            select((value + 1) % checkBoxList.getValueCount());
            GuiHelper.playClickSound();
        }

        public String getDisplayName() {
            return displayName;
        }

        public CheckBoxEntry setDisplayName(String name) {
            displayName = name;
            return this;
        }

        public CheckBoxEntry setLocked(boolean state) {
            locked = state;
            return this;
        }

        public void addMouseOverText(List<String> list) {}

        public CheckBoxEntry select(int v) {
            if (locked) return this;
            if (checkBoxList.radioButtons) {
                if (v > 0) {
                    for (CheckBoxEntry entry : checkBoxList.getActiveEntries()) {
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
    protected final List<CheckBoxEntry> entries;

    public CheckBoxList(Panel gui, boolean radiobutton) {
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
        if (value > 0) {
            GuiIcons.BLANK.withColor(Color4I.BLACK.withAlpha(150)).draw(x, y, w, h);
        }
    }

    public void addBox(CheckBoxEntry checkBox) {
        checkBox.checkBoxList = this;
        entries.add(checkBox);
        entries.sort((e1, e2) -> e1.name.compareToIgnoreCase(e2.name));
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
        CheckBoxEntry entry = getEntryUnderMouse();
        if (entry != null) {
            entry.onClicked(button, getActiveEntries().indexOf(entry));
        }
    }

    @Override
    public void addMouseOverText(List<String> list) {
        CheckBoxEntry entry = getEntryUnderMouse();

        if (entry != null) {
            entry.addMouseOverText(list);
        }
    }

    public List<CheckBoxEntry> getActiveEntries() {
        if (getGui() instanceof GuiButtonListBase btnList && btnList.hasSearchBox()
                && !btnList.getTextInSearchBox().isEmpty()) {
            return entries.stream().filter(entry -> entry.name.toLowerCase().contains(btnList.getTextInSearchBox()))
                    .collect(Collectors.toList());
        }
        return entries;
    }

    @Override
    public void draw(Theme theme, int x, int y, int w, int h) {
        drawBackground(theme, x, y, w, h);

        for (int i = 0; i < getActiveEntries().size(); i++) {
            CheckBoxEntry entry = getActiveEntries().get(i);
            int ey = y + i * 11 + 1;
            drawCheckboxBackground(theme, x, ey, 10, 10);
            getCheckboxIcon(theme, x + 1, ey + 1, 8, 8, i, entry.value);
            if (parent.isMouseOver && getEntryUnderMouse() == entry) {
                theme.drawString(
                        entry.getDisplayName(),
                        x + 12,
                        ey + 1,
                        theme.getContentColor(WidgetType.mouseOver(true)),
                        Theme.SHADOW);
            } else {
                theme.drawString(entry.getDisplayName(), x + 12, ey + 1, Theme.SHADOW);
            }
            GlStateManager.color(1F, 1F, 1F, 1F);
        }
    }

    public CheckBoxEntry getEntryUnderMouse() {
        int y = getMouseY() - getY();

        if (y % 11 == 10) {
            return null;
        }

        int i = y / 11;

        if (i >= 0 && i < getActiveEntries().size()) {
            return getActiveEntries().get(i);
        }

        return null;
    }
}
