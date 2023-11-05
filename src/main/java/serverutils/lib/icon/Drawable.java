package serverutils.lib.icon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.client.GlStateManager;

public interface Drawable {

    @SideOnly(Side.CLIENT)
    void draw(int x, int y, int w, int h);

    @SideOnly(Side.CLIENT)
    default void drawStatic(int x, int y, int w, int h) {
        draw(x, y, w, h);
    }

    @SideOnly(Side.CLIENT)
    default void draw3D() {
        GlStateManager.pushMatrix();
        GlStateManager.scale(1D / 16D, 1D / 16D, 1D);
        draw(-8, -8, 16, 16);
        GlStateManager.popMatrix();
    }
}
