package serverutils.lib;

import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;

import serverutils.lib.icon.Color4I;
import serverutils.lib.util.EnumDyeColorHelper;
import serverutils.lib.util.IStringSerializable;
import serverutils.lib.util.misc.NameMap;

public enum EnumTeamColor implements IStringSerializable {

    BLUE("blue", EnumDyeColor.BLUE, EnumChatFormatting.BLUE, 0x0094FF),
    CYAN("cyan", EnumDyeColor.CYAN, EnumChatFormatting.AQUA, 0x00DDFF),
    GREEN("green", EnumDyeColor.GREEN, EnumChatFormatting.GREEN, 0x23D34C),
    YELLOW("yellow", EnumDyeColor.YELLOW, EnumChatFormatting.YELLOW, 0xFFD000),
    ORANGE("orange", EnumDyeColor.ORANGE, EnumChatFormatting.GOLD, 0xFF9400),
    RED("red", EnumDyeColor.RED, EnumChatFormatting.RED, 0xEA4B4B),
    PINK("pink", EnumDyeColor.PINK, EnumChatFormatting.LIGHT_PURPLE, 0xE888C9),
    MAGENTA("magenta", EnumDyeColor.MAGENTA, EnumChatFormatting.LIGHT_PURPLE, 0xFF007F),
    PURPLE("purple", EnumDyeColor.PURPLE, EnumChatFormatting.DARK_PURPLE, 0xB342FF),
    GRAY("gray", EnumDyeColor.GRAY, EnumChatFormatting.GRAY, 0xC0C0C0);

    public static final NameMap<EnumTeamColor> NAME_MAP = NameMap.createWithNameAndColor(
            BLUE,
            (sender, value) -> new ChatComponentTranslation(value.getLangKey()),
            EnumTeamColor::getColor,
            values());

    private final String name;
    private final EnumDyeColor dyeColor;
    private final EnumChatFormatting formatting;
    private final Color4I color;
    private final String langKey;

    EnumTeamColor(String n, EnumDyeColor d, EnumChatFormatting t, int c) {
        name = n;
        dyeColor = d;
        formatting = t;
        color = Color4I.rgb(c);
        langKey = EnumDyeColorHelper.get(dyeColor).getLangKey();
    }

    @Override
    public String getName() {
        return name;
    }

    public EnumChatFormatting getEnumChatFormatting() {
        return formatting;
    }

    public Color4I getColor() {
        return color;
    }

    public EnumDyeColor getDyeColor() {
        return dyeColor;
    }

    public String getLangKey() {
        return langKey;
    }
}
