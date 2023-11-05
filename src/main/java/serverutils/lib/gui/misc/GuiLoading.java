package serverutils.lib.gui.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;

import serverutils.lib.client.GlStateManager;
import serverutils.lib.gui.GuiBase;
import serverutils.lib.gui.GuiHelper;
import serverutils.lib.gui.Theme;
import serverutils.lib.icon.Color4I;

public class GuiLoading extends GuiBase {

    private boolean startedLoading = false;
    private boolean isLoading = true;
    private String title;
    public float timer;

    public GuiLoading() {
        this("");
    }

    public GuiLoading(String t) {
        setSize(128, 128);
        title = t;
    }

    @Override
    public void addWidgets() {}

    @Override
    public void drawBackground(Theme theme, int x, int y, int w, int h) {
        if (!startedLoading) {
            startLoading();
            startedLoading = true;
        }

        if (isLoading()) {
            GuiHelper.drawHollowRect(x + width / 2 - 48, y + height / 2 - 8, 96, 16, Color4I.WHITE, true);

            int x1 = x + width / 2 - 48;
            int y1 = y + height / 2 - 8;
            int w1 = 96;
            int h1 = 16;

            Color4I col = Color4I.WHITE;
            GlStateManager.disableTexture2D();
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();

            GuiHelper.addRectToBuffer(tessellator, x1, y1 + 1, 1, h1 - 2, col);
            GuiHelper.addRectToBuffer(tessellator, x1 + w1 - 1, y1 + 1, 1, h1 - 2, col);
            GuiHelper.addRectToBuffer(tessellator, x1 + 1, y1, w1 - 2, 1, col);
            GuiHelper.addRectToBuffer(tessellator, x1 + 1, y1 + h1 - 1, w1 - 2, 1, col);

            x1 += 1;
            y1 += 1;
            w1 -= 2;
            h1 -= 2;

            timer += Minecraft.getMinecraft().timer.ticksPerSecond;

            for (int oy = 0; oy < h1; oy++) {
                for (int ox = 0; ox < w1; ox++) {
                    int index = ox + oy + (int) timer;

                    if (index % (h1 * 2) < h1) {
                        col = Color4I.WHITE.withAlpha(200 - (index % h1) * 9);

                        GuiHelper.addRectToBuffer(tessellator, x1 + ox, y1 + oy, 1, 1, col);
                    }
                }
            }

            tessellator.draw();
            GlStateManager.enableTexture2D();

            String s = getTitle();

            if (!s.isEmpty()) {
                String[] s1 = s.split("\n");

                for (int i = 0; i < s1.length; i++) {
                    theme.drawString(s1[i], x + width / 2, y - 26 + i * 12, Theme.CENTERED);
                }
            }
        } else {
            closeGui();
            finishLoading();
        }
    }

    @Override
    public synchronized String getTitle() {
        return title;
    }

    public synchronized void setTitle(String s) {
        title = s;
    }

    public synchronized void setFinished() {
        isLoading = false;
    }

    public void startLoading() {}

    public synchronized boolean isLoading() {
        return isLoading;
    }

    public void finishLoading() {}
}
