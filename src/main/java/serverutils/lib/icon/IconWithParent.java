package serverutils.lib.icon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public abstract class IconWithParent extends Icon {

    public final Icon parent;

    public IconWithParent(Icon i) {
        parent = i;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void bindTexture() {
        parent.bindTexture();
    }
}
