package serverutils.client.gui.misc;

import net.minecraft.client.gui.GuiPlayerInfo;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiPlayerInfoWrapper extends GuiPlayerInfo {

    public String displayName;

    public GuiPlayerInfoWrapper(GuiPlayerInfo info, String displayName) {
        super(info.name);
        this.responseTime = info.responseTime;
        this.displayName = displayName;
    }
}
