package serverutils.lib.gui.misc;

import net.minecraft.client.resources.I18n;

import serverutils.lib.gui.CheckBoxList;
import serverutils.lib.gui.GuiBase;
import serverutils.lib.gui.Panel;
import serverutils.lib.gui.PanelScrollBar;
import serverutils.lib.gui.TextBox;
import serverutils.lib.gui.Theme;
import serverutils.lib.gui.Widget;
import serverutils.lib.gui.WidgetLayout;

public abstract class GuiButtonListBase extends GuiBase {

    protected final Panel panelButtons;
    protected final PanelScrollBar scrollBar;
    private String title = "";
    private TextBox searchBox;
    private boolean hasSearchBox;
    private int borderH, borderV, borderW;

    public GuiButtonListBase() {
        panelButtons = new Panel(this) {

            @Override
            public void add(Widget widget) {
                if (!hasSearchBox || searchBox.getText().isEmpty()
                        || getFilterText(widget).contains(searchBox.getText().toLowerCase())
                        || widget instanceof CheckBoxList) {
                    super.add(widget);
                }
            }

            @Override
            public void addWidgets() {
                addButtons(this);
            }

            @Override
            public void alignWidgets() {
                setY(hasSearchBox ? 23 : 9);
                int prevWidth = width;

                if (widgets.isEmpty()) {
                    setWidth(100);
                } else {
                    setWidth(100);

                    for (Widget w : widgets) {
                        setWidth(Math.max(width, w.width));
                    }
                }

                if (hasSearchBox) {
                    setWidth(Math.max(width, prevWidth));
                }

                for (Widget w : widgets) {
                    w.setX(borderH);
                    w.setWidth(width - borderH * 2);
                }

                setHeight(140);

                scrollBar.setPosAndSize(posX + width + 6, posY - 1, 16, height + 2);
                scrollBar.setMaxValue(align(new WidgetLayout.Vertical(borderV, borderW, borderV)));

                getGui().setWidth(scrollBar.posX + scrollBar.width + 8);
                getGui().setHeight(height + 18 + (hasSearchBox ? 14 : 0));

                if (hasSearchBox) {
                    searchBox.setPosAndSize(8, 6, getGui().width - 16, 12);
                }
            }

            @Override
            public void drawBackground(Theme theme, int x, int y, int w, int h) {
                theme.drawPanelBackground(x, y, w, h);
            }
        };

        panelButtons.setPosAndSize(9, 9, 0, 146);

        scrollBar = new PanelScrollBar(this, panelButtons);
        scrollBar.setCanAlwaysScroll(true);
        scrollBar.setScrollStep(20);

        searchBox = new TextBox(this) {

            @Override
            public void onTextChanged() {
                panelButtons.refreshWidgets();
            }
        };

        searchBox.ghostText = I18n.format("gui.search_box");
        hasSearchBox = false;
    }

    public void setHasSearchBox(boolean v) {
        if (hasSearchBox != v) {
            hasSearchBox = v;
            refreshWidgets();
        }
    }

    public String getFilterText(Widget widget) {
        return widget.getTitle().toLowerCase();
    }

    @Override
    public void addWidgets() {
        add(panelButtons);
        add(scrollBar);

        if (hasSearchBox) {
            add(searchBox);
        }
    }

    @Override
    public void alignWidgets() {
        panelButtons.alignWidgets();
    }

    public abstract void addButtons(Panel panel);

    public void setTitle(String txt) {
        title = txt;
    }

    @Override
    public String getTitle() {
        return title;
    }

    public void setBorder(int h, int v, int w) {
        borderH = h;
        borderV = v;
        borderW = w;
    }

    @Override
    public void drawBackground(Theme theme, int x, int y, int w, int h) {
        super.drawBackground(theme, x, y, w, h);

        String title = getTitle();

        if (!title.isEmpty()) {
            theme.drawString(
                    title,
                    x + (width - theme.getStringWidth(title)) / 2,
                    y - theme.getFontHeight() - 2,
                    Theme.SHADOW);
        }
    }

    public boolean hasSearchBox() {
        return hasSearchBox;
    }

    public String getTextInSearchBox() {
        return searchBox.getText().toLowerCase();
    }

    public void focus() {
        searchBox.setFocused(true);
    }
}
