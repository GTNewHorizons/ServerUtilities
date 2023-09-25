package serverutils.old.mod.client.gui.guide;

import java.util.List;

import net.minecraft.util.IChatComponent;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.api.client.GlStateManager;
import serverutils.lib.api.gui.GuiLM;
import serverutils.lib.api.gui.widgets.ButtonLM;
import serverutils.old.api.guide.lines.GuideTextLine;

/**
 * Created by LatvianModder on 04.03.2016.
 */
@SideOnly(Side.CLIENT)
public class ButtonGuideTextLine extends ButtonLM {

    public final GuiGuide guiGuide;
    public List<String> text;

    public ButtonGuideTextLine(GuiGuide g, GuideTextLine l) {
        super(g, 0, g.panelText.height, 0, 0);
        guiGuide = g;

        if (l != null) {
            IChatComponent c = l.getText();

            if (c != null) {
                text = guiGuide.getFontRenderer().listFormattedStringToWidth(c.getFormattedText(), g.panelText.width);
                if (text.isEmpty()) text = null;
            }
        }

        if (text != null) {
            if (text.size() > 1) width = g.panelText.width;
            else width = g.getFontRenderer().getStringWidth(text.get(0));
            height = 10 * text.size();
        } else {
            width = 0;
            height = 11;
        }
    }

    public void addMouseOverText(List<String> l) {}

    public void onButtonPressed(int b) {}

    public void renderWidget() {
        int ay = getAY();
        if (ay < -height || ay > guiGuide.mainPanel.height) return;
        int ax = getAX();

        boolean mouseOver = mouseOver();

        if (text != null) {
            for (int i = 0; i < text.size(); i++) {
                guiGuide.getFontRenderer().drawString(text.get(i), ax, ay + i * 10, guiGuide.colorText);
            }
        }

        if (mouseOver) {
            GlStateManager.color(0F, 0F, 0F, 0.101F);
            GuiLM.drawBlankRect(ax, ay, guiGuide.getZLevel(), width, height);
        }
    }
}
