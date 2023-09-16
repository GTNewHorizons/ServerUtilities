package serverutils.utils.mod.client.gui.guide;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.IChatComponent;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.api.client.GlStateManager;
import serverutils.lib.api.gui.GuiLM;
import serverutils.utils.api.guide.lines.GuideExtendedTextLine;

/**
 * Created by LatvianModder on 04.03.2016.
 */
@SideOnly(Side.CLIENT)
public class ButtonGuideExtendedTextLine extends ButtonGuideTextLine {

    public final GuideExtendedTextLine line;
    public List<String> hover;

    public ButtonGuideExtendedTextLine(GuiGuide g, GuideExtendedTextLine l) {
        super(g, l);
        line = l;

        if (l != null) {
            List<IChatComponent> h = l.getHover();

            if (h != null) {
                hover = new ArrayList<>();

                for (IChatComponent c1 : h) {
                    hover.add(c1.getFormattedText());
                }

                if (hover.isEmpty()) hover = null;
            } else hover = null;
        }
    }

    public void addMouseOverText(List<String> l) {
        if (hover != null) l.addAll(hover);
    }

    public void onButtonPressed(int b) {
        if (line != null) line.onClicked();
    }

    public void renderWidget() {
        int ay = getAY();
        if (ay < -height || ay > guiGuide.mainPanel.height) return;
        int ax = getAX();

        boolean mouseOver = mouseOver();

        if (text != null && !text.isEmpty()) {
            for (int i = 0; i < text.size(); i++) {
                gui.getFontRenderer().drawString(text.get(i), ax, ay + i * 10, guiGuide.colorText);
            }
        }

        if (mouseOver) {
            GlStateManager.color(0F, 0F, 0F, 0.101F);
            GuiLM.drawBlankRect(ax, ay, gui.getZLevel(), width, height);
        }
    }
}
