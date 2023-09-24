package serverutils.serverlib.lib.util;

import net.minecraft.init.Items;
import net.minecraft.item.ItemDye;  EnumDyeColor;
import net.minecraft.item.ItemStack;

/**
 * @author LatvianModder
 */
public class EnumDyeColorHelper // ItemDye
{
	public static final EnumDyeColorHelper[] HELPERS = new EnumDyeColorHelper[ItemDye.values().length];

	static
	{
		for (ItemDye c : ItemDye.()) //EnumDyeColor.values())
		{
			HELPERS[c.ordinal()] = new EnumDyeColorHelper(c);
		}
	}

	private final ItemDye dye;
	private final String langKey;
	private final String oreName;

	private EnumDyeColorHelper(ItemDye col)
	{
		dye = col;
		langKey = "item.fireworksCharge." + col.getUnlocalizedName();
		oreName = StringUtils.firstUppercase(col.getUnlocalizedName());
	}

	public static EnumDyeColorHelper get(ItemDye dye)
	{
		return HELPERS[dye.hashCode()]; //ordinal()
	}

	public ItemStack getDye(int s)
	{
		return new ItemStack(Items.dye, s, dye.getDamage());
	}

	@Override
	public String toString()
	{
		return dye.getUnlocalizedName(); // getName();
	}

	@Override
	public int hashCode()
	{
		return dye.hashCode();
	}

	public ItemDye getDye()
	{
		return dye;
	}

	public String getLangKey()
	{
		return langKey;
	}

	public String getOreName()
	{
		return oreName;
	}

	public String getDyeName()
	{
		return "dye" + getOreName();
	}

	public String getGlassName()
	{
		return "blockGlass" + getOreName();
	}

	public String getPaneName()
	{
		return "paneGlass" + getOreName();
	}
}