package serverutils.lib.gui.misc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.ChunkCoordIntPair;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import serverutils.lib.client.CachedVertexData;
import serverutils.lib.client.GlStateManager;
import serverutils.lib.gui.Button;
import serverutils.lib.gui.GuiBase;
import serverutils.lib.gui.GuiHelper;
import serverutils.lib.gui.Panel;
import serverutils.lib.gui.Theme;
import serverutils.lib.gui.Widget;
import serverutils.lib.gui.WidgetLayout;
import serverutils.lib.icon.Color4I;
import serverutils.lib.math.MathUtils;
import serverutils.lib.util.misc.MouseButton;

public class GuiChunkSelectorBase extends GuiBase {

    protected enum Corner {
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        TOP_LEFT
    }

    public static final int TILE_SIZE = 12;
    private static final CachedVertexData GRID = new CachedVertexData(GL11.GL_LINES, false, true, false);

    static {
        GRID.color.set(128, 128, 128, 50);

        for (int x = 0; x <= ChunkSelectorMap.TILES_GUI; x++) {
            GRID.pos(x * TILE_SIZE, 0D);
            GRID.pos(x * TILE_SIZE, ChunkSelectorMap.TILES_GUI * TILE_SIZE, 0D);
        }

        for (int y = 0; y <= ChunkSelectorMap.TILES_GUI; y++) {
            GRID.pos(0D, y * TILE_SIZE, 0D);
            GRID.pos(ChunkSelectorMap.TILES_GUI * TILE_SIZE, y * TILE_SIZE, 0D);
        }
    }

    public static class MapButton extends Button {

        public final GuiChunkSelectorBase gui;
        public final ChunkCoordIntPair chunkPos;
        public final int index;
        private boolean isSelected = false;

        private MapButton(GuiChunkSelectorBase g, int i) {
            super(g);
            gui = g;
            index = i;
            setPosAndSize(
                    (index % ChunkSelectorMap.TILES_GUI) * TILE_SIZE,
                    (index / ChunkSelectorMap.TILES_GUI) * TILE_SIZE,
                    TILE_SIZE,
                    TILE_SIZE);
            chunkPos = new ChunkCoordIntPair(
                    gui.startX + (i % ChunkSelectorMap.TILES_GUI),
                    gui.startZ + (i / ChunkSelectorMap.TILES_GUI));
        }

        @Override
        public void onClicked(MouseButton button) {
            GuiHelper.playClickSound();
            gui.currentSelectionMode = gui.getSelectionMode(button);

            if (gui.blockMode) {
                gui.anchorIndex = index;
                gui.mouseIndex = index;
            }

            if (gui.currentSelectionMode == -1) {
                gui.onChunksSelected(Collections.singleton(chunkPos));
            }
        }

        @Override
        public void addMouseOverText(List<String> list) {
            gui.addButtonText(this, list);
        }

        @Override
        public void draw(Theme theme, int x, int y, int w, int h) {
            if (gui.currentSelectionMode != -1) {
                if (gui.blockMode && gui.anchorIndex >= 0 && gui.mouseIndex >= 0) {
                    if (gui.isMouseOver(this)) {
                        gui.mouseIndex = index;
                    }
                    ChunkCoordIntPair anchorChunk = gui.mapButtons[gui.anchorIndex].chunkPos;
                    ChunkCoordIntPair mouseChunk = gui.mapButtons[gui.mouseIndex].chunkPos;
                    isSelected = MathUtils
                            .isNumberBetween(this.chunkPos.chunkXPos, anchorChunk.chunkXPos, mouseChunk.chunkXPos)
                            && MathUtils.isNumberBetween(
                                    this.chunkPos.chunkZPos,
                                    anchorChunk.chunkZPos,
                                    mouseChunk.chunkZPos);
                } else {
                    if (!isSelected && gui.isMouseOver(this)) {
                        isSelected = true;
                    }
                }
            }
            if (isSelected || gui.isMouseOver(this)) {
                Color4I.WHITE.withAlpha(33).draw(x, y, TILE_SIZE, TILE_SIZE);
            }
        }
    }

    public int startX, startZ;
    private final MapButton[] mapButtons;
    private final Panel panelButtons;
    public int currentSelectionMode = -1;
    public boolean blockMode = false;
    public int anchorIndex = -1;
    public int mouseIndex = -1;

    public GuiChunkSelectorBase() {
        startX = MathUtils.chunk(Minecraft.getMinecraft().thePlayer.posX) - ChunkSelectorMap.TILES_GUI2;
        startZ = MathUtils.chunk(Minecraft.getMinecraft().thePlayer.posZ) - ChunkSelectorMap.TILES_GUI2;

        panelButtons = new Panel(this) {

            @Override
            public void addWidgets() {
                addCornerButtons(panelButtons);
            }

            @Override
            public void alignWidgets() {
                int h = align(WidgetLayout.VERTICAL);
                int w = 0;

                for (Widget widget : widgets) {
                    w = Math.max(w, widget.width);
                }

                panelButtons.setPosAndSize(getGui().width + 2, -2, w, h);
            }
        };

        mapButtons = new MapButton[ChunkSelectorMap.TILES_GUI * ChunkSelectorMap.TILES_GUI];

        for (int i = 0; i < mapButtons.length; i++) {
            mapButtons[i] = new MapButton(this, i);
        }
    }

    @Override
    public boolean onInit() {
        ChunkSelectorMap.getMap().resetMap(startX, startZ);
        return true;
    }

    @Override
    public void addWidgets() {
        for (MapButton b : mapButtons) {
            add(b);
        }

        add(panelButtons);
    }

    @Override
    public void alignWidgets() {
        setSize(ChunkSelectorMap.TILES_GUI * TILE_SIZE, ChunkSelectorMap.TILES_GUI * TILE_SIZE);
        panelButtons.alignWidgets();
    }

    @Override
    public void drawBackground(Theme theme, int x, int y, int w, int h) {
        int currentStartX = MathUtils.chunk(Minecraft.getMinecraft().thePlayer.posX) - ChunkSelectorMap.TILES_GUI2;
        int currentStartZ = MathUtils.chunk(Minecraft.getMinecraft().thePlayer.posZ) - ChunkSelectorMap.TILES_GUI2;

        if (currentStartX != startX || currentStartZ != startZ) {
            startX = currentStartX;
            startZ = currentStartZ;

            for (int i = 0; i < mapButtons.length; i++) {
                mapButtons[i] = new MapButton(this, i);
            }

            ChunkSelectorMap.getMap().resetMap(startX, startZ);
        }

        GlStateManager.color(1F, 1F, 1F, 1F);
        Color4I.BLACK.draw(x - 2, y - 2, w + 4, h + 4);

        ChunkSelectorMap.getMap().drawMap(this, x, y, startX, startZ);

        GlStateManager.color(1F, 1F, 1F, 1F);

        for (MapButton mapButton : mapButtons) {
            mapButton.draw(theme, mapButton.getX(), mapButton.getY(), mapButton.width, mapButton.height);
        }

        GlStateManager.disableTexture2D();
        GlStateManager.glLineWidth(1F);

        Tessellator tessellator = Tessellator.instance;
        tessellator.setTranslation(mapButtons[0].getX(), mapButtons[0].getY(), 0D);
        // GlStateManager.color(1F, 1F, 1F, GuiScreen.isCtrlKeyDown() ? 0.2F : 0.7F);
        GlStateManager.color(1F, 1F, 1F, 1F);

        if (!isKeyDown(Keyboard.KEY_TAB)) {
            drawArea(tessellator);
        }

        GRID.draw(tessellator);
        tessellator.setTranslation(0D, 0D, 0D);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    @Override
    public void mouseReleased(MouseButton button) {
        super.mouseReleased(button);

        if (currentSelectionMode != -1) {
            Collection<ChunkCoordIntPair> c = new ArrayList<>();

            for (MapButton b : mapButtons) {
                if (b.isSelected) {
                    c.add(b.chunkPos);
                    b.isSelected = false;
                }
            }

            onChunksSelected(c);
            currentSelectionMode = -1;
            anchorIndex = -1;
            mouseIndex = -1;
            blockMode = false;
        }
    }

    @Override
    public void drawForeground(Theme theme, int x, int y, int w, int h) {
        int lineSpacing = theme.getFontHeight() + 1;
        List<String> tempTextList = new ArrayList<>();
        addCornerText(tempTextList, Corner.BOTTOM_RIGHT);

        for (int i = 0; i < tempTextList.size(); i++) {
            String s = tempTextList.get(i);
            theme.drawString(
                    s,
                    getScreen().getScaledWidth() - theme.getStringWidth(s) - 2,
                    getScreen().getScaledHeight() - (tempTextList.size() - i) * lineSpacing,
                    Theme.SHADOW);
        }

        tempTextList.clear();

        addCornerText(tempTextList, Corner.BOTTOM_LEFT);

        for (int i = 0; i < tempTextList.size(); i++) {
            theme.drawString(
                    tempTextList.get(i),
                    2,
                    getScreen().getScaledHeight() - (tempTextList.size() - i) * lineSpacing,
                    Theme.SHADOW);
        }

        tempTextList.clear();

        addCornerText(tempTextList, Corner.TOP_LEFT);

        for (int i = 0; i < tempTextList.size(); i++) {
            theme.drawString(tempTextList.get(i), 2, 2 + i * lineSpacing, Theme.SHADOW);
        }

        super.drawForeground(theme, x, y, w, h);
    }

    public int getSelectionMode(MouseButton button) {
        return -1;
    }

    public void onChunksSelected(Collection<ChunkCoordIntPair> chunks) {}

    public void drawArea(Tessellator tessellator) {}

    public void addCornerButtons(Panel panel) {}

    public void addCornerText(List<String> list, Corner corner) {}

    public void addButtonText(MapButton button, List<String> list) {}
}
