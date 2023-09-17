package serverutils.utils.mod.client.gui.friends;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.api.client.GlStateManager;
import serverutils.lib.api.gui.GuiLM;
import serverutils.utils.api.guide.GuidePage;
import serverutils.utils.api.guide.lines.GuideTextLine;
import serverutils.utils.mod.client.gui.guide.ButtonGuideTextLine;
import serverutils.utils.mod.client.gui.guide.GuiGuide;
import serverutils.utils.world.LMPlayerClient;

/**
 * Created by LatvianModder on 23.03.2016.
 */
@SideOnly(Side.CLIENT)
public class GuidePlayerInventoryLine extends GuideTextLine {

    public final LMPlayerClient playerLM;

    public GuidePlayerInventoryLine(GuidePage c, LMPlayerClient p) {
        super(c, null);
        playerLM = p;
    }

    @SideOnly(Side.CLIENT)
    public ButtonGuideTextLine createWidget(GuiGuide gui) {
        return new ButtonGuidePlayerInventory(gui, this);
    }

    public class ButtonGuidePlayerInventory extends ButtonGuideTextLine {

        public ButtonGuidePlayerInventory(GuiGuide g, GuidePlayerInventoryLine w) {
            super(g, null);
            width = 18 * 9;
            height = 18 * 4 + 4;
        }

        public void addMouseOverText(List<String> l) {}

        public void onButtonPressed(int b) {}

        public void renderWidget() {
            int ay = getAY();
            if (ay < -height || ay > guiGuide.mainPanel.height) return;
            int ax = getAX();
            float z = gui.getZLevel();

            GlStateManager.color(1F, 1F, 1F, 0.2F);
            GuiLM.drawBlankRect(ax, ay, z, width, height);

            for (int i = 0; i < 9 * 3; i++) {
                GuiLM.drawBlankRect(ax + (i % 9) * 18 + 1, ay + (i / 9) * 18 + 1, z, 16, 16);
            }

            for (int i = 0; i < 9; i++) {
                GuiLM.drawBlankRect(ax + i * 18 + 1, ay + 18 * 3 + 5, z, 16, 16);
            }

            GlStateManager.color(1F, 1F, 1F, 1F);

            EntityPlayer ep = playerLM.getPlayer();

            if (ep != null) {
                for (int i = 0; i < ep.inventory.mainInventory.length - 9; i++) {
                    GuiLM.drawItem(
                            gui,
                            ep.inventory.mainInventory[i + 9],
                            ax + (i % 9) * 18 + 1,
                            ay + (i / 9) * 18 + 1);
                }

                for (int i = 0; i < 9; i++) {
                    GuiLM.drawItem(gui, ep.inventory.mainInventory[i], ax + i * 18 + 1, ay + 18 * 3 + 5);
                }
            }
        }
    }
}
