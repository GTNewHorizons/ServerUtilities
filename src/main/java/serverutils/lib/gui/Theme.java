package serverutils.lib.gui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.IChatComponent;

import serverutils.ServerUtilities;
import serverutils.lib.client.GlStateManager;
import serverutils.lib.icon.Color4I;
import serverutils.lib.icon.Icon;
import serverutils.lib.icon.ImageIcon;
import serverutils.lib.icon.PartIcon;
import serverutils.lib.io.Bits;

public class Theme {

    public static final Theme DEFAULT = new Theme();
    public static boolean renderDebugBoxes = false;

    public static final int DARK = 1;
    public static final int SHADOW = 2;
    public static final int CENTERED = 4;
    public static final int UNICODE = 8;
    public static final int MOUSE_OVER = 16;
    public static final int CENTERED_V = 32;

    private static final Color4I CONTENT_COLOR_MOUSE_OVER = Color4I.rgb(16777120);
    private static final Color4I CONTENT_COLOR_DISABLED = Color4I.rgb(10526880);
    private static final Color4I CONTENT_COLOR_DARK = Color4I.rgb(4210752);

    public static final ImageIcon BACKGROUND_SQUARES = (ImageIcon) Icon
            .getIcon(ServerUtilities.MOD_ID + ":textures/gui/background_squares.png");
    private static final ImageIcon TEXTURE_BEACON = (ImageIcon) Icon.getIcon("textures/gui/container/beacon.png");
    private static final ImageIcon TEXTURE_WIDGETS = (ImageIcon) Icon.getIcon("textures/gui/widgets.png");
    private static final ImageIcon TEXTURE_TABS = (ImageIcon) Icon
            .getIcon("textures/gui/container/creative_inventory/tabs.png");
    private static final ImageIcon TEXTURE_ENCHANTING_TABLE = (ImageIcon) Icon
            .getIcon("textures/gui/container/enchanting_table.png");

    private static final Icon GUI = new PartIcon(TEXTURE_TABS, 0, 97, 28, 28, 4);
    private static final Icon GUI_MOUSE_OVER = GUI.withTint(Color4I.rgb(0xAFB6DA));

    private static final Icon BUTTON = new PartIcon(TEXTURE_WIDGETS, 0, 66, 200, 20, 4);
    private static final Icon BUTTON_MOUSE_OVER = new PartIcon(TEXTURE_WIDGETS, 0, 86, 200, 20, 4);
    private static final Icon BUTTON_DISABLED = new PartIcon(TEXTURE_WIDGETS, 0, 46, 200, 20, 4);

    private static final Icon WIDGET = new PartIcon(TEXTURE_BEACON, 0, 219, 22, 22, 6);
    private static final Icon WIDGET_MOUSE_OVER = new PartIcon(TEXTURE_BEACON, 66, 219, 22, 22, 6);
    private static final Icon WIDGET_DISABLED = new PartIcon(TEXTURE_BEACON, 44, 219, 22, 22, 6);

    private static final Icon SLOT = new PartIcon(TEXTURE_BEACON, 35, 136, 18, 18, 3);
    private static final Icon SLOT_MOUSE_OVER = SLOT.combineWith(Color4I.WHITE.withAlpha(33));

    private static final Icon SCROLL_BAR_BG = SLOT;
    private static final Icon SCROLL_BAR_BG_DISABLED = SCROLL_BAR_BG.withTint(Color4I.BLACK.withAlpha(100));

    private static final Icon TEXT_BOX = new PartIcon(TEXTURE_ENCHANTING_TABLE, 0, 185, 108, 19, 6);

    private static final Icon TAB_H_UNSELECTED = TEXTURE_TABS.withUV(0, 97, 28, 28, 256, 256);
    private static final Icon TAB_H_SELECTED = TEXTURE_TABS.withUV(0, 64, 27, 27, 256, 256);

    private final Deque<Boolean> fontUnicode = new ArrayDeque<>();

    public Color4I getContentColor(WidgetType type) {
        return type == WidgetType.MOUSE_OVER ? CONTENT_COLOR_MOUSE_OVER
                : type == WidgetType.DISABLED ? CONTENT_COLOR_DISABLED : Color4I.WHITE;
    }

    public Color4I getInvertedContentColor() {
        return CONTENT_COLOR_DARK;
    }

    public void drawGui(int x, int y, int w, int h, WidgetType type) {
        (type == WidgetType.MOUSE_OVER ? GUI_MOUSE_OVER : GUI).draw(x, y, w, h);
    }

    public void drawWidget(int x, int y, int w, int h, WidgetType type) {
        (type == WidgetType.MOUSE_OVER ? WIDGET_MOUSE_OVER : type == WidgetType.DISABLED ? WIDGET_DISABLED : WIDGET)
                .draw(x, y, w, h);
    }

    public void drawSlot(int x, int y, int w, int h, WidgetType type) {
        (type == WidgetType.MOUSE_OVER ? SLOT_MOUSE_OVER : SLOT).draw(x, y, w, h);
    }

    public void drawContainerSlot(int x, int y, int w, int h) {
        SLOT.draw(x - 1, y - 1, w + 2, h + 2);
    }

    public void drawButton(int x, int y, int w, int h, WidgetType type) {
        (type == WidgetType.MOUSE_OVER ? BUTTON_MOUSE_OVER : type == WidgetType.DISABLED ? BUTTON_DISABLED : BUTTON)
                .draw(x, y, w, h);
    }

    public void drawScrollBarBackground(int x, int y, int w, int h, WidgetType type) {
        (type == WidgetType.DISABLED ? SCROLL_BAR_BG_DISABLED : SCROLL_BAR_BG).draw(x, y, w, h);
    }

    public void drawScrollBar(int x, int y, int w, int h, WidgetType type, boolean vertical) {
        (type == WidgetType.MOUSE_OVER ? WIDGET_MOUSE_OVER : WIDGET).draw(x + 1, y + 1, w - 2, h - 2);
    }

    public void drawTextBox(int x, int y, int w, int h) {
        TEXT_BOX.draw(x, y, w, h);
    }

    public void drawCheckboxBackground(int x, int y, int w, int h, boolean radioButton) {
        drawSlot(x, y, w, h, WidgetType.NORMAL);
    }

    public void drawCheckbox(int x, int y, int w, int h, WidgetType type, boolean selected, boolean radioButton) {
        if (selected) {
            drawWidget(x, y, w, h, type);
        }
    }

    public void drawPanelBackground(int x, int y, int w, int h) {
        drawContainerSlot(x, y, w, h);
    }

    public void drawHorizontalTab(int x, int y, int w, int h, boolean selected) {
        (selected ? TAB_H_SELECTED : TAB_H_UNSELECTED).draw(x, y, w, h);
    }

    public void drawContextMenuBackground(int x, int y, int w, int h) {
        drawGui(x, y, w, h, WidgetType.NORMAL);
        Color4I.BLACK.withAlpha(90).draw(x, y, w, h);
    }

    public FontRenderer getFont() {
        return Minecraft.getMinecraft().fontRenderer;
    }

    public final int getStringWidth(String text) {
        return getFont().getStringWidth(text);
    }

    public final int getFontHeight() {
        return getFont().FONT_HEIGHT;
    }

    public final String trimStringToWidth(String text, int width) {
        return text.isEmpty() ? "" : getFont().trimStringToWidth(text, width, false);
    }

    public final String trimStringToWidthReverse(String text, int width) {
        return text.isEmpty() ? "" : getFont().trimStringToWidth(text, width, true);
    }

    public final List<String> listFormattedStringToWidth(String text, int width) {
        if (width <= 0 || text.isEmpty()) {
            return Collections.emptyList();
        }

        return getFont().listFormattedStringToWidth(text, width);
    }

    public final int drawString(String text, int x, int y, Color4I color, int flags) {
        if (text.isEmpty() || color.isEmpty()) {
            return 0;
        }

        if (Bits.getFlag(flags, CENTERED)) {
            x -= getStringWidth(text) / 2;
        }

        int i = getFont().drawString(text, x, y, color.rgba(), Bits.getFlag(flags, SHADOW));
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        return i;
    }

    public final int drawString(String text, int x, int y, int flags) {
        return drawString(text, x, y, getContentColor(WidgetType.mouseOver(Bits.getFlag(flags, MOUSE_OVER))), flags);
    }

    public final int drawString(String text, int x, int y) {
        return drawString(text, x, y, getContentColor(WidgetType.NORMAL), 0);
    }

    public void pushFontUnicode(boolean flag) {
        fontUnicode.push(getFont().getUnicodeFlag());
        getFont().setUnicodeFlag(flag);
    }

    public void popFontUnicode() {
        getFont().setUnicodeFlag(fontUnicode.pop());
    }

    public List<GuiBase.PositionedTextData> createDataFrom(IChatComponent component, int width) {
        if (width <= 0 || component.getUnformattedText().isEmpty()) {
            return Collections.emptyList();
        }

        List<GuiBase.PositionedTextData> list = new ArrayList<>();

        int line = 0;
        int currentWidth = 0;

        for (IChatComponent t : (Collection<IChatComponent>) component.createCopy()) {
            String text = t.getUnformattedTextForChat();
            int textWidth = getStringWidth(text);

            while (textWidth > 0) {
                int w = textWidth;
                if (w > width - currentWidth) {
                    w = width - currentWidth;
                }

                list.add(new GuiBase.PositionedTextData(currentWidth, line * 10, w, 10, t.getChatStyle()));

                currentWidth += w;
                textWidth -= w;

                if (currentWidth >= width) {
                    currentWidth = 0;
                    line++;
                }
            }
        }

        return list;
    }
}
