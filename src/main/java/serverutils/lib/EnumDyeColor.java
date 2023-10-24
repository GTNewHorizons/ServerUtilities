package serverutils.lib;

import net.minecraft.client.resources.I18n;
import net.minecraft.init.Items;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public enum EnumDyeColor {

    BLACK("Black", 0xFF3F3F3F, EnumChatFormatting.BLACK),
    RED("Red", 0xFFFF0000, EnumChatFormatting.DARK_RED),
    GREEN("Green", 0xFF009B0E, EnumChatFormatting.DARK_GREEN),
    BROWN("Brown", 0xFFA35C2D, EnumChatFormatting.GOLD),
    BLUE("Blue", 0xFF004CC4, EnumChatFormatting.DARK_BLUE),
    PURPLE("Purple", 0xFF9A41E2, EnumChatFormatting.DARK_PURPLE),
    CYAN("Cyan", 0xFF00D8C6, EnumChatFormatting.DARK_AQUA),
    LIGHT_GRAY("LightGray", 0xFFBCBCBC, EnumChatFormatting.GRAY),
    GRAY("Gray", 0xFF636363, EnumChatFormatting.DARK_GRAY),
    PINK("Pink", 0xFFFF95A3, EnumChatFormatting.LIGHT_PURPLE),
    LIME("Lime", 0xFF00FF2E, EnumChatFormatting.GREEN),
    YELLOW("Yellow", 0xFFFFD500, EnumChatFormatting.YELLOW),
    LIGHT_BLUE("LightBlue", 0xFF63BEFF, EnumChatFormatting.BLUE),
    MAGENTA("Magenta", 0xFFFF006E, EnumChatFormatting.AQUA),
    ORANGE("Orange", 0xFFFF9500, EnumChatFormatting.GOLD),
    WHITE("White", 0xFFFFFFFF, EnumChatFormatting.WHITE);

    public static final EnumDyeColor[] VALUES = values();

    public final int ID;
    public final String lang;
    public final String name;
    public final String unlocalizedName;
    public final int color;
    public final int colorBright;
    public final EnumChatFormatting chatFormatting;

    public final String dyeName;
    public final String glassName;
    public final String paneName;

    EnumDyeColor(String s, int c, EnumChatFormatting f) {
        ID = ordinal();
        name = ItemDye.field_150921_b[ID];
        unlocalizedName = ItemDye.field_150923_a[ID];
        lang = "serverutilities.color." + s.toLowerCase();
        color = ItemDye.field_150922_c[ID];
        colorBright = c;
        chatFormatting = f;

        dyeName = "dye" + s;
        glassName = "blockGlass" + s;
        paneName = "paneGlass" + s;
    }

    @SideOnly(Side.CLIENT)
    public String toString() {
        return I18n.format(lang);
    }

    public ItemStack getDye(int s) {
        return new ItemStack(Items.dye, s, ID);
    }
}
