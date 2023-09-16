package ftb.lib.mod.client.gui;

import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ftb.lib.api.client.GlStateManager;
import ftb.lib.api.gui.GuiLM;
import ftb.lib.api.item.ItemDisplay;

@SideOnly(Side.CLIENT)
public class GuiDisplayItem extends GuiLM {

    public static final ResourceLocation texture = new ResourceLocation("ftbl", "textures/gui/displayitem.png");

    public ItemDisplay itemDisplay;

    public GuiDisplayItem(ItemDisplay i) {
        super(null, texture);
        mainPanel.width = 144;
        mainPanel.height = 182;
        itemDisplay = i;
    }

    public void addWidgets() {}

    public void drawBackground() {
        super.drawBackground();

        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.pushMatrix();
        GlStateManager.translate(mainPanel.posX + mainPanel.width / 2F, mainPanel.posY + mainPanel.height / 2F, 0F);
        GlStateManager.scale(itemDisplay.scale, itemDisplay.scale, 1F);
        itemDisplay.item.stackSize = 1;
        GuiLM.drawItem(this, itemDisplay.item, -8, -8);
        GlStateManager.popMatrix();
    }

    public void drawText(List<String> l) {
        if (itemDisplay.title != null && !itemDisplay.title.isEmpty()) drawCenteredString(
                fontRendererObj,
                itemDisplay.title,
                mainPanel.posX + mainPanel.width / 2,
                mainPanel.posY + 6,
                0xFFFFFFFF);
        if (itemDisplay.desc != null && !itemDisplay.desc.isEmpty()) l.addAll(itemDisplay.desc);
        super.drawText(l);
    }

    public boolean handleDragNDrop(GuiContainer g, int x, int y, ItemStack is, int b) {
        if (is != null && x > mainPanel.posX
                && x < mainPanel.posX + mainPanel.width
                && y > mainPanel.posY
                && y < mainPanel.posY + mainPanel.height) {
            itemDisplay = new ItemDisplay(is.copy(), is.getDisplayName(), null, itemDisplay.scale);
            is.stackSize = 0;
            return true;
        }

        return false;
    }
}
