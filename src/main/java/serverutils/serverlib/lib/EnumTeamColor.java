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
	BLUE("blue", EnumChatFormatting.BLUE, EnumChatFormatting.BLUE, 0x0094FF),
	CYAN("cyan", EnumChatFormatting.AQUA, EnumChatFormatting.AQUA, 0x00DDFF),
	GREEN("green", EnumChatFormatting.GREEN, EnumChatFormatting.GREEN, 0x23D34C),
	YELLOW("yellow", EnumChatFormatting.YELLOW, EnumChatFormatting.YELLOW, 0xFFD000),
	ORANGE("orange", EnumChatFormatting.GOLD, EnumChatFormatting.GOLD, 0xFF9400),
	RED("red", EnumChatFormatting.RED, EnumChatFormatting.RED, 0xEA4B4B),
	PINK("pink", EnumChatFormatting.LIGHT_PURPLE, EnumChatFormatting.LIGHT_PURPLE, 0xE888C9),
	MAGENTA("magenta", EnumChatFormatting.LIGHT_PURPLE, EnumChatFormatting.LIGHT_PURPLE, 0xFF007F),
	PURPLE("purple", EnumChatFormatting.DARK_PURPLE, EnumChatFormatting.DARK_PURPLE, 0xB342FF),
	GRAY("gray", EnumChatFormatting.GRAY, EnumChatFormatting.GRAY, 0xC0C0C0);

	public static final NameMap<EnumTeamColor> NAME_MAP = NameMap.createWithNameAndColor(BLUE, (sender, value) -> new ChatComponentTranslation(value.getLangKey()), EnumTeamColor::getColor, values());

	private final String name;
	private final EnumChatFormatting dyeColor;
	private final EnumChatFormatting textFormatting;
	private final Color4I color;
	private final String langKey;

	EnumTeamColor(String n, EnumChatFormatting d, EnumChatFormatting t, int c)
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

	public EnumChatFormatting getTextFormatting()
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