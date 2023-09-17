package serverutils.lib.api;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import latmod.lib.util.FinalIDObject;

/**
 * Created by LatvianModder on 29.03.2016.
 */
public class LangKey extends FinalIDObject {

    public LangKey(String s) {
        super(s);
    }

    @SideOnly(Side.CLIENT)
    public String format(Object... o) {
        return I18n.format(getID(), o);
    }

    public IChatComponent chatComponent(Object... o) {
        return new ChatComponentTranslation(getID(), o);
    }
}
