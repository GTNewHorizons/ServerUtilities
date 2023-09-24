package serverutils.serverlib.lib.util.misc;

import serverutils.serverlib.lib.gui.GuiIcons;
import serverutils.serverlib.lib.icon.Icon;
import net.minecraft.util.IStringSerializable;

/**
 * @author LatvianModder
 */
public enum EnumRedstoneMode implements IStringSerializable
{
	DISABLED("disabled"),
	ACTIVE_HIGH("active_high"),
	ACTIVE_LOW("active_low"),
	PULSE("pulse");

	public static final NameMap<EnumRedstoneMode> NAME_MAP = NameMap.createWithBaseTranslationKey(DISABLED, "redstone_mode", DISABLED, ACTIVE_HIGH, ACTIVE_LOW);
	public static final NameMap<EnumRedstoneMode> NAME_MAP_WITH_PULSE = NameMap.createWithBaseTranslationKey(DISABLED, "redstone_mode", DISABLED, ACTIVE_HIGH, ACTIVE_LOW, PULSE);

	private final String name;

	EnumRedstoneMode(String n)
	{
		name = n;
	}

	@Override
	public String getName()
	{
		return name;
	}

	public boolean isActive(boolean prevValue, boolean value)
	{
		switch (this)
		{
			case DISABLED:
				return false;
			case ACTIVE_HIGH:
				return value;
			case ACTIVE_LOW:
				return !value;
			default:
				return false;
		}
	}

	public Icon getIcon()
	{
		switch (this)
		{
			case ACTIVE_HIGH:
				return GuiIcons.RS_HIGH;
			case ACTIVE_LOW:
				return GuiIcons.RS_LOW;
			case PULSE:
				return GuiIcons.RS_PULSE;
			default:
				return GuiIcons.RS_NONE;
		}
	}
}