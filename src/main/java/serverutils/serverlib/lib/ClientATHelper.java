package serverutils.serverlib.lib;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.chat.IChatListener;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ChatType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public final class ClientATHelper
{
	@Nullable
	public static ResourceLocation getFontUnicodePage(int page)
	{
		return FontRenderer.UNICODE_PAGE_LOCATIONS[page];
	}

	public static Map<ChatType, List<IChatListener>> getChatListeners()
	{
		return Minecraft.getMinecraft().ingameGUI.chatListeners;
	}

	public static Map<String, TextureAtlasSprite> getRegisteredSpritesMap()
	{
		return Minecraft.getMinecraft().getTextureMapBlocks().mapRegisteredSprites;
	}
}