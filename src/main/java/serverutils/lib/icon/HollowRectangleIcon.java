package serverutils.lib.icon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.gui.GuiHelper;

public class HollowRectangleIcon extends Icon {

    public Color4I color;
    public boolean roundEdges;

    public HollowRectangleIcon(Color4I c, boolean r) {
        color = c;
        roundEdges = r;
    }

    @Override
    public HollowRectangleIcon copy() {
        return new HollowRectangleIcon(color, roundEdges);
    }

    @Override
    public HollowRectangleIcon withColor(Color4I color) {
        return new HollowRectangleIcon(color, roundEdges);
    }

    @Override
    public HollowRectangleIcon withTint(Color4I c) {
        return withColor(color.withTint(c));
    }

    @Override
    protected void setProperties(IconProperties properties) {
        super.setProperties(properties);
        roundEdges = properties.getBoolean("round_edges", roundEdges);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void draw(int x, int y, int w, int h) {
        GuiHelper.drawHollowRect(x, y, w, h, color, roundEdges);
    }

    @Override
    public JsonElement getJson() {
        JsonObject o = new JsonObject();
        o.addProperty("id", "hollow_rectangle");
        o.add("color", color.getJson());

        if (roundEdges) {
            o.addProperty("round_edges", true);
        }

        return o;
    }
}
