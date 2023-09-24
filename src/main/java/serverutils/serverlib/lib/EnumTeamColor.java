package serverutils.serverlib.lib;

import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import serverutils.serverlib.lib.icon.Color4I;
import serverutils.serverlib.lib.util.EnumDyeColorHelper;
import serverutils.serverlib.lib.util.misc.NameMap;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

/**
 * @author LatvianModder
 */
public enum EnumTeamColor implements IStringSerializable
{
	BLUE("blue", EnumChatFormatting.BLUE, ChatFormatting.BLUE, 0x0094FF),
	CYAN("cyan", EnumChatFormatting.AQUA, ChatFormatting.AQUA, 0x00DDFF),
	GREEN("green", EnumChatFormatting.GREEN, ChatFormatting.GREEN, 0x23D34C),
	YELLOW("yellow", EnumChatFormatting.YELLOW, ChatFormatting.YELLOW, 0xFFD000),
	ORANGE("orange", EnumChatFormatting.GOLD, ChatFormatting.GOLD, 0xFF9400),
	RED("red", EnumChatFormatting.RED, ChatFormatting.RED, 0xEA4B4B),
	PINK("pink", EnumChatFormatting.LIGHT_PURPLE, ChatFormatting.LIGHT_PURPLE, 0xE888C9),
	MAGENTA("magenta", EnumChatFormatting.LIGHT_PURPLE, ChatFormatting.LIGHT_PURPLE, 0xFF007F),
	PURPLE("purple", EnumChatFormatting.DARK_PURPLE, ChatFormatting.DARK_PURPLE, 0xB342FF),
	GRAY("gray", EnumChatFormatting.GRAY, ChatFormatting.GRAY, 0xC0C0C0);

	public static final NameMap<EnumTeamColor> NAME_MAP = NameMap.createWithNameAndColor(BLUE, (sender, value) -> new ChatComponentTranslation(value.getLangKey()), EnumTeamColor::getColor, values());

	private final String name;
	private final EnumChatFormatting dyeColor;
	private final ChatFormatting textFormatting;
	private final Color4I color;
	private final String langKey;

	EnumTeamColor(String n, EnumChatFormatting d, ChatFormatting t, int c)
	{
		name = n;
		dyeColor = d;
		textFormatting = t;
		color = Color4I.rgb(c);
		langKey = EnumDyeColorHelper.get(dyeColor).getLangKey();
	}

	@Override
	public String getName()
	{
		return name;
	}

	public ChatFormatting getTextFormatting()
	{
		return textFormatting;
	}

	public Color4I getColor()
	{
		return color;
	}

	public EnumChatFormatting getDyeColor()
	{
		return dyeColor;
	}

	public String getLangKey()
	{
		return langKey;
	}
}