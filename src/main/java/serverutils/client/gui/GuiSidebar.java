package serverutils.client.gui;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.resources.I18n;

import org.lwjgl.opengl.GL11;

import serverutils.client.ServerUtilitiesClientConfig;
import serverutils.lib.client.ClientUtils;
import serverutils.lib.client.GlStateManager;
import serverutils.lib.icon.Color4I;

public class GuiSidebar extends GuiButton {

    public static Rectangle lastDrawnArea = new Rectangle();
    private final GuiContainer gui;
    public final List<GuiButtonSidebar> buttons;
    private GuiButtonSidebar mouseOver;

    public GuiSidebar(GuiContainer g) {
        super(495829, 0, 0, 0, 0, "");
        gui = g;
        buttons = new ArrayList<>();
    }

    @Override
    public void drawButton(Minecraft mc, int mx, int my) {
        buttons.clear();
        mouseOver = null;
        int rx = 0, ry = 0;
        boolean addedAny;
        boolean top = ServerUtilitiesClientConfig.sidebar_buttons.top();
        boolean above = ServerUtilitiesClientConfig.sidebar_buttons.above();
        boolean vertical = ServerUtilitiesClientConfig.sidebar_buttons.vertical();

        for (SidebarButtonGroup group : SidebarButtonManager.INSTANCE.groups) {
            if (above && !ClientUtils.isCreativePlusGui(gui)) {
                // If drawn above they are drawn in a horizontal line of 7 buttons
                // roughly the same length as a potion label.
                for (SidebarButton button : group.getButtons()) {
                    if (button.isActuallyVisible()) {
                        buttons.add(new GuiButtonSidebar(rx, ry, button));
                        ry++;
                        if (ry >= 7) {
                            ry = 0;
                            rx--;
                        }
                    }
                }
            } else if (vertical) {
                for (SidebarButton button : group.getButtons()) {
                    if (button.isActuallyVisible()) {
                        buttons.add(new GuiButtonSidebar(rx, ry, button));
                        rx++;
                        if (rx >= 9) {
                            rx = 0;
                            ry++;
                        }
                    }
                }
            } else {
                rx = 0;
                addedAny = false;
                for (SidebarButton button : group.getButtons()) {
                    if (button.isActuallyVisible()) {
                        buttons.add(new GuiButtonSidebar(rx, ry, button));
                        rx++;
                        addedAny = true;
                    }
                }

                if (addedAny) {
                    ry++;
                }
            }
        }

        int guiLeft = gui.guiLeft;
        int guiTop = gui.guiTop;

        if (top) {
            for (GuiButtonSidebar button : buttons) {
                button.x = 1 + button.buttonX * 17;
                button.y = 1 + button.buttonY * 17;
            }
        } else {
            int offsetX = 18;
            int offsetY = 8;

            if (gui instanceof GuiContainerCreative) {
                offsetY = 6;
            }

            if (above) {
                offsetX = 22;
                offsetY = -18;
            }

            if (ClientUtils.isCreativePlusGui(gui)) {
                offsetY = 22;
                offsetX = 41;
            }

            for (GuiButtonSidebar button : buttons) {
                button.x = guiLeft - offsetX - button.buttonY * 17;
                button.y = guiTop + offsetY + button.buttonX * 17;
            }
        }

        int x = Integer.MAX_VALUE;
        int y = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (GuiButtonSidebar b : buttons) {
            if (b.x >= 0 && b.y >= 0) {
                x = Math.min(x, b.x);
                y = Math.min(y, b.y);
                maxX = Math.max(maxX, b.x + 16);
                maxY = Math.max(maxY, b.y + 16);
            }

            if (mx >= b.x && my >= b.y && mx < b.x + 16 && my < b.y + 16) {
                mouseOver = b;
            }
        }

        x -= 2;
        y -= 2;
        maxX += 2;
        maxY += 2;

        width = maxX - x;
        height = maxY - y;
        zLevel = 0F;

        xPosition = x;
        yPosition = y;

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0, 500);

        FontRenderer font = mc.fontRenderer;

        GlStateManager.enableBlend();
        GlStateManager.enableDepth();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1F, 1F, 1F, 1F);

        for (GuiButtonSidebar b : buttons) {
            b.button.getIcon().draw(b.x, b.y, 16, 16);

            if (b == mouseOver) {
                Color4I.WHITE.withAlpha(33).draw(b.x, b.y, 16, 16);
            }

            if (b.button.getCustomTextHandler() != null) {
                String text = b.button.getCustomTextHandler().get();

                if (!text.isEmpty()) {
                    int nw = font.getStringWidth(text);
                    int width = 16;
                    Color4I.LIGHT_RED.draw(b.x + width - nw, b.y - 1, nw + 1, 9);
                    font.drawString(text, b.x + width - nw + 1, b.y, 0xFFFFFFFF);
                    GlStateManager.color(1F, 1F, 1F, 1F);
                }
            }
        }

        if (mouseOver != null) {
            int mx1 = mx + 10;
            int my1 = Math.max(3, my - 9);

            List<String> list = new ArrayList<>();
            list.add(I18n.format(mouseOver.button.getLangKey()));

            if (mouseOver.button.getTooltipHandler() != null) {
                mouseOver.button.getTooltipHandler().accept(list);
            }

            int tw = 0;

            for (String s : list) {
                tw = Math.max(tw, font.getStringWidth(s));
            }

            GlStateManager.pushMatrix();
            GlStateManager.translate(0, 0, 500);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            Color4I.DARK_GRAY.draw(mx1 - 3, my1 - 2, tw + 6, 2 + list.size() * 10);

            for (int i = 0; i < list.size(); i++) {
                font.drawString(list.get(i), mx1, my1 + i * 10, 0xFFFFFFFF);
            }
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.popMatrix();
        }

        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.disableDepth();
        GlStateManager.popMatrix();
        zLevel = 0F;

        lastDrawnArea = new Rectangle(xPosition, yPosition, width, height);
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mx, int my) {
        if (super.mousePressed(mc, mx, my)) {
            if (mouseOver != null) {
                mouseOver.button.onClicked(GuiScreen.isShiftKeyDown());
            }
            return true;
        }
        return false;
    }

    public static class GuiButtonSidebar {

        public final int buttonX, buttonY;
        public final SidebarButton button;
        public int x, y;

        public GuiButtonSidebar(int x, int y, SidebarButton b) {
            buttonX = x;
            buttonY = y;
            button = b;
        }
    }
}
