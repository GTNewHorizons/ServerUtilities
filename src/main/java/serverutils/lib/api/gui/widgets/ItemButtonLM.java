package serverutils.lib.api.gui.widgets;

import net.minecraft.item.ItemStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.TextureCoords;
import serverutils.lib.api.gui.GuiLM;
import serverutils.lib.api.gui.IGuiLM;

@SideOnly(Side.CLIENT)
public abstract class ItemButtonLM extends ButtonLM {

    public ItemStack item;

    public ItemButtonLM(IGuiLM g, int x, int y, int w, int h, ItemStack is) {
        super(g, x, y, w, h);
        item = is;
    }

    public ItemButtonLM(IGuiLM g, int x, int y, int w, int h) {
        this(g, x, y, w, h, null);
    }

    public void setItem(ItemStack is) {
        item = is;
    }

    public void setBackground(TextureCoords t) {
        background = t;
    }

    public void renderWidget() {
        if (item != null) GuiLM.drawItem(gui, item, getAX(), getAY());
    }
}
