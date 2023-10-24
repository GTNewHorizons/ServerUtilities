package serverutils.lib.gui.misc;

import serverutils.lib.gui.GuiBase;

public abstract class ChunkSelectorMap {

    private static ChunkSelectorMap INSTANCE;

    public static ChunkSelectorMap getMap() {
        return INSTANCE;
    }

    public static void setMap(ChunkSelectorMap map) {
        INSTANCE = map;
    }

    public static final int TILES_TEX = 16;
    public static final int TILES_GUI = 15;
    public static final int TILES_GUI2 = TILES_GUI / 2;

    public abstract void resetMap(int startX, int startZ);

    public abstract void drawMap(GuiBase gui, int ax, int ay, int startX, int startZ);
}
