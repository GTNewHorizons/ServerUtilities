package serverutils.lib.api.gui.widgets;

import java.util.List;

import net.minecraft.client.gui.GuiScreen;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.api.client.GlStateManager;
import serverutils.lib.api.client.ServerUtilitiesLibraryClient;
import serverutils.lib.api.gui.GuiLM;
import serverutils.lib.api.gui.IGuiLM;

@SideOnly(Side.CLIENT)
public abstract class ButtonSimpleLM extends ButtonLM {

    public int colorText = 0xFFFFFFFF;
    public int colorButton = 0xFF888888;
    public int colorButtonOver = 0xFF999999;

    public ButtonSimpleLM(IGuiLM g, int x, int y, int w, int h) {
        super(g, x, y, w, h);
    }

    public void addMouseOverText(List<String> l) {}

    public void renderWidget() {
        int ax = getAX();
        int ay = getAY();
        ServerUtilitiesLibraryClient.setGLColor(mouseOver(ax, ay) ? colorButtonOver : colorButton);
        GuiLM.drawBlankRect(ax, ay, gui.getZLevel(), width, height);
        GlStateManager.color(1F, 1F, 1F, 1F);
        ((GuiScreen) gui).drawCenteredString(
                gui.getFontRenderer(),
                title,
                ax + width / 2,
                ay + (height - gui.getFontRenderer().FONT_HEIGHT) / 2,
                colorText);
    }
}
