package serverutils.lib;

import javax.annotation.Nullable;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;

public final class ClientATHelper {

    @Nullable
    public static ResourceLocation getFontUnicodePage(int page) {
        return FontRenderer.unicodePageLocations[page];
    }
}
