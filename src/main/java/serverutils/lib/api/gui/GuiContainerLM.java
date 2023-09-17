package serverutils.lib.api.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.api.client.GlStateManager;
import serverutils.lib.api.client.ServerUtilitiesLibraryClient;
import serverutils.lib.api.gui.widgets.PanelLM;

/**
 * Created by LatvianModder on 30.01.2016.
 */
@SideOnly(Side.CLIENT)
public abstract class GuiContainerLM extends GuiContainer implements IGuiLM {

    private static final ArrayList<String> tempTextList = new ArrayList<>();

    private boolean refreshWidgets = true;
    public final ResourceLocation texture;
    public final ContainerLM container;
    public final PanelLM mainPanel;
    private final MouseLM mouse;
    public float delta;

    public GuiContainerLM(ContainerLM c, ResourceLocation tex) {
        super(c);

        texture = tex;
        container = c;
        mc = ServerUtilitiesLibraryClient.mc;

        ScaledResolution scr = new ScaledResolution(
                mc,
                ServerUtilitiesLibraryClient.mc.displayWidth,
                ServerUtilitiesLibraryClient.mc.displayHeight);

        mainPanel = new PanelLM(this, 0, 0, scr.getScaledWidth(), scr.getScaledHeight()) {

            public void addWidgets() {
                GuiContainerLM.this.addWidgets();
            }
        };

        mainPanel.width = 176;
        mainPanel.height = 166;

        mouse = new MouseLM();
        refreshWidgets();
    }

    public void refreshWidgets() {
        refreshWidgets = true;
    }

    public final void initGui() {
        super.initGui();
        initLMGui();
        xSize = mainPanel.width;
        ySize = mainPanel.height;
        guiLeft = mainPanel.posX = (width - mainPanel.width) / 2;
        guiTop = mainPanel.posY = (height - mainPanel.height) / 2;
        refreshWidgets();
    }

    public void initLMGui() {}

    public abstract void addWidgets();

    public ItemStack getHeldItem() {
        return mc.thePlayer.inventory.getItemStack();
    }

    public GuiScreen getGui() {
        return this;
    }

    public PanelLM getMainPanel() {
        return mainPanel;
    }

    public MouseLM mouse() {
        return mouse;
    }

    public final float getZLevel() {
        return zLevel;
    }

    public final void setZLevel(float z) {
        zLevel = z;
    }

    public FontRenderer getFontRenderer() {
        return fontRendererObj;
    }

    public final void close(GuiScreen g) {
        ServerUtilitiesLibraryClient.openGui(g);
    }

    protected final void drawGuiContainerBackgroundLayer(float f, int mx, int my) {}

    protected final void mouseClicked(int mx, int my, int b) {
        mouse.onClicked(b, true);
        mainPanel.mousePressed(b);
        super.mouseClicked(mx, my, b);
        mouseClicked();
    }

    public void mouseClicked() {}

    protected void keyTyped(char keyChar, int key) {
        if (mainPanel.keyPressed(key, keyChar)) return;
        if (key == 1 || key == mc.gameSettings.keyBindInventory.getKeyCode()) onClosedByKey();
        super.keyTyped(keyChar, key);
    }

    public void onClosedByKey() {
        close(null);
    }

    public void drawTexturedModalRect(int x, int y, int u, int v, int w, int h) {
        drawTexturedModalRectD(x, y, u, v, w, h);
    }

    public void drawTexturedModalRectD(double x, double y, double u, double v, double w, double h) {
        GuiLM.drawTexturedModalRectD(x, y, zLevel, u, v, w, h, 256, 256);
    }

    public void drawBackground() {
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.color(1F, 1F, 1F, 1F);

        if (texture != null) {
            ServerUtilitiesLibraryClient.setTexture(texture);
            drawTexturedModalRectD(mainPanel.posX, mainPanel.posY, 0D, 0D, mainPanel.width, mainPanel.height);
        }
    }

    public void drawForeground() {}

    public final void drawScreen(int mx, int my, float f) {
        mouse.onUpdate(mx, my);
        delta = f;

        if (refreshWidgets) {
            mainPanel.refreshWidgets();
            refreshWidgets = false;
        }

        GlStateManager.disableLighting();
        GlStateManager.color(1F, 1F, 1F, 1F);
        drawDefaultBackground();
        drawBackground();
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        super.drawScreen(mx, my, f);
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        tempTextList.clear();
        drawText(tempTextList);
        if (!tempTextList.isEmpty()) drawHoveringText(tempTextList, mouse.x, Math.max(mouse.y, 18), fontRendererObj);
        GlStateManager.disableLighting();
        drawForeground();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    public void drawText(List<String> l) {
        mainPanel.addMouseOverText(l);
    }

    public final void onGuiClosed() {
        super.onGuiClosed();
        onLMGuiClosed();
    }

    public void onLMGuiClosed() {}
}
