package serverutils.lib.icon;

import net.minecraft.client.renderer.Tessellator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.client.GlStateManager;
import serverutils.lib.gui.GuiHelper;

public class BulletIcon extends Icon {

    private static final MutableColor4I DEFAULT_COLOR = Color4I.rgb(0xEDEDED).mutable();
    private static final MutableColor4I DEFAULT_COLOR_B = Color4I.rgb(0xFFFFFF).mutable();
    private static final MutableColor4I DEFAULT_COLOR_D = Color4I.rgb(0xDDDDDD).mutable();

    private Color4I color, colorB, colorD;
    private boolean inverse;

    public BulletIcon() {
        color = EMPTY;
        colorB = EMPTY;
        colorD = EMPTY;
        inverse = false;
    }

    @Override
    public BulletIcon copy() {
        BulletIcon icon = new BulletIcon();
        icon.color = color;
        icon.colorB = colorB;
        icon.colorD = colorD;
        icon.inverse = inverse;
        return icon;
    }

    public BulletIcon setColor(Color4I col) {
        color = col;

        if (color.isEmpty()) {
            return this;
        }

        MutableColor4I c = color.mutable();
        c.addBrightness(18);
        colorB = c.copy();
        c = color.mutable();
        c.addBrightness(-18);
        colorD = c.copy();
        return this;
    }

    @Override
    public BulletIcon withColor(Color4I col) {
        return copy().setColor(col);
    }

    @Override
    public BulletIcon withTint(Color4I c) {
        return withColor(color.withTint(c));
    }

    public BulletIcon setInverse(boolean v) {
        inverse = v;
        return this;
    }

    @Override
    protected void setProperties(IconProperties properties) {
        super.setProperties(properties);
        inverse = properties.getBoolean("inverse", inverse);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void draw(int x, int y, int w, int h) {
        Color4I c, cb, cd;

        if (color.isEmpty()) {
            c = DEFAULT_COLOR;
            cb = DEFAULT_COLOR_B;
            cd = DEFAULT_COLOR_D;
        } else {
            c = color;
            cb = colorB;
            cd = colorD;
        }

        GlStateManager.disableTexture2D();
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        GuiHelper.addRectToBuffer(tessellator, x, y + 1, 1, h - 2, inverse ? cd : cb);
        GuiHelper.addRectToBuffer(tessellator, x + w - 1, y + 1, 1, h - 2, inverse ? cb : cd);
        GuiHelper.addRectToBuffer(tessellator, x + 1, y, w - 2, 1, inverse ? cd : cb);
        GuiHelper.addRectToBuffer(tessellator, x + 1, y + h - 1, w - 2, 1, inverse ? cb : cd);
        GuiHelper.addRectToBuffer(tessellator, x + 1, y + 1, w - 2, h - 2, c);

        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    @Override
    public JsonElement getJson() {
        JsonObject o = new JsonObject();
        o.addProperty("id", "bullet");

        if (!color.isEmpty()) {
            o.add("color", color.getJson());
        }

        return o;
    }
}
