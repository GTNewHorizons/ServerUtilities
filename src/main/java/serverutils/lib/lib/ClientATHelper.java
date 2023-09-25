package serverutils.lib.lib;

import javax.annotation.Nullable;

import net.minecraft.util.ResourceLocation;

public final class ClientATHelper {

    private static final ResourceLocation[] unicodePageLocations = new ResourceLocation[256];

    private static ResourceLocation getUnicodePageLocation(int page) {
        if (unicodePageLocations[page] == null) {
            unicodePageLocations[page] = new ResourceLocation(
                    String.format("textures/font/unicode_page_%02x.png", page));
        }
        return unicodePageLocations[page];
    }

    @Nullable
    public static ResourceLocation getFontUnicodePage(int page) {
        return ClientATHelper.getUnicodePageLocation(page); // FontRenderer.unicodePageLocations[page];
    }

    // public static Map<ChatType, List<IChatListener>> getChatListeners()
    // {
    // return Minecraft.getMinecraft().ingameGUI.chatListeners;
    // }

    // TODO:Fix
    /*
     * public static Map<String, TextureAtlasSprite> getRegisteredSpritesMap() { return
     * Minecraft.getMinecraft().getTextureMapBlocks().mapRegisteredSprites; }
     */
}
