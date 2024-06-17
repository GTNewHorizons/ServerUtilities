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
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import serverutils.client.EnumPlacement;
import serverutils.client.EnumSidebarLocation;
import serverutils.client.ServerUtilitiesClientConfig;
import serverutils.lib.client.ClientUtils;
import serverutils.lib.client.GlStateManager;
import serverutils.lib.icon.Color4I;

public class GuiSidebar extends GuiButton {

    public static Rectangle lastDrawnArea = new Rectangle();
    public static int dragOffsetX = 0, dragOffsetY = 0;
    private final GuiContainer gui;
    public final List<GuiButtonSidebar> buttons;
    private int dragStartX, dragStartY;
    private GuiButtonSidebar mouseOver;
    private boolean isDragging;
    private final EnumSidebarLocation location;
    private EnumPlacement placement;

    public GuiSidebar(GuiContainer g) {
        super(495829, 0, 0, 0, 0, "");
        gui = g;
        buttons = new ArrayList<>();
        location = ServerUtilitiesClientConfig.sidebar_buttons;
        placement = ServerUtilitiesClientConfig.sidebar_placement;
    }

    @Override
    public void drawButton(Minecraft mc, int mx, int my) {
        mouseOver = null;
        if (ServerUtilitiesClientConfig.sidebar_buttons != location) {
            clearButtons();
        }

        this.setButtonLocations(mc, mx, my);
        if (buttons.isEmpty()) {
            addButtonsToSidebar();
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
            list.add(StatCollector.translateToLocal(mouseOver.button.getLangKey()));

            if (mouseOver.button.isDisabled()) {
                list.add(EnumChatFormatting.RED + ClientUtils.getDisabledTip());
            }

            if (mouseOver.button.getTooltipHandler() != null) {
                mouseOver.button.getTooltipHandler().accept(list);
            }

            int tw = 0;

            for (String s : list) {
                tw = Math.max(tw, font.getStringWidth(s));
            }

            Color4I.DARK_GRAY.draw(mx1 - 3, my1 - 2, tw + 6, 2 + list.size() * 10);

            for (int i = 0; i < list.size(); i++) {
                font.drawString(list.get(i), mx1, my1 + i * 10, 0xFFFFFFFF);
            }
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
                if (GuiScreen.isCtrlKeyDown() && !location.isLocked()) {
                    this.isDragging = true;
                    this.dragStartX = mx;
                    this.dragStartY = my;
                } else {
                    mouseOver.button.onClicked(GuiScreen.isShiftKeyDown());
                }
                return true;
            }
        }
        return false;
    }

    public void mouseDragged(Minecraft mc, int mx, int my) {
        if (isDragging) {
            int newDragOffsetX = mx - dragStartX;
            int newDragOffsetY = my - dragStartY;

            for (GuiButtonSidebar button : buttons) {
                int newX = button.x + newDragOffsetX;
                int newY = button.y + newDragOffsetY;

                if (newX < 0 || newX + 16 > mc.currentScreen.width || newY < 0 || newY + 16 > mc.currentScreen.height) {
                    return;
                }
            }

            dragOffsetX += newDragOffsetX;
            dragOffsetY += newDragOffsetY;
            dragStartX = mx;
            dragStartY = my;
        }
    }

    @Override
    public void mouseReleased(int mx, int my) {
        if (isDragging) {
            this.isDragging = false;
            SidebarButtonManager.INSTANCE.saveConfig();
        }
    }

    private void addButtonsToSidebar() {
        int rx = 0, ry = 0;
        int max = placement.getMaxInRow();

        for (SidebarButtonGroup group : SidebarButtonManager.INSTANCE.groups) {
            boolean addedAny = false;
            for (SidebarButton button : group.getButtons()) {
                if (!button.isActuallyVisible()) continue;
                buttons.add(new GuiButtonSidebar(rx, ry, button));
                switch (placement) {
                    case HORIZONTAL -> ry++;
                    case VERTICAL -> rx++;
                    case GROUPED -> {
                        rx++;
                        addedAny = true;
                    }
                };

                if (placement != EnumPlacement.GROUPED) {
                    if (ry >= max) {
                        ry = 0;
                        rx--;
                    } else if (rx >= max) {
                        rx = 0;
                        ry++;
                    }
                }
            }
            if (addedAny) {
                rx = 0;
                ry++;
            }
        }
    }

    private void setButtonLocations(Minecraft mc, int mx, int my) {
        int offsetX = 18;
        int offsetY = 8;

        if (gui instanceof GuiContainerCreative) {
            offsetY = 6;
        }

        if (location.above()) {
            placement = ClientUtils.isCreativePlusGui(gui) ? placement : EnumPlacement.HORIZONTAL;
            offsetX = 22;
            offsetY = -18;
        }

        if (ClientUtils.isCreativePlusGui(gui)) {
            offsetY = 22;
            offsetX = 41;
        }

        this.mouseDragged(mc, mx, my);
        int actualOffsetX = location.isLocked() ? offsetX : offsetX - dragOffsetX;
        int actualOffsetY = location.isLocked() ? offsetY : -offsetY - dragOffsetY;
        int maxOffsetX = mc.currentScreen.width - 16;
        int maxOffsetY = mc.currentScreen.height - 16;

        for (GuiButtonSidebar button : buttons) {
            if (location == EnumSidebarLocation.TOP_LEFT) {
                button.x = 1 + button.buttonX * 17;
                button.y = 1 + button.buttonY * 17;
            } else if (location.isLocked()) {
                button.x = gui.guiLeft - offsetX - button.buttonY * 17;
                button.y = gui.guiTop + offsetY + button.buttonX * 17;
            } else {
                button.x = MathHelper.clamp_int((gui.guiLeft - button.buttonY * 17) - actualOffsetX, 0, maxOffsetX);
                button.y = MathHelper.clamp_int((gui.guiTop + button.buttonX * 17) - actualOffsetY, 0, maxOffsetY);
            }
        }
    }

    public void clearButtons() {
        buttons.clear();
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
