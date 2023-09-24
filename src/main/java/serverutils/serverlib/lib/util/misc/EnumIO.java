package serverutils.serverlib.lib.util.misc;

import serverutils.serverlib.lib.gui.GuiIcons;
import serverutils.serverlib.lib.icon.Icon;
import net.minecraft.util.IStringSerializable;

/**
 * @author LatvianModder
 */
public enum EnumIO implements IStringSerializable
{
	IO("io"),
	IN("in"),
	OUT("out"),
	NONE("none");

	public static final NameMap<EnumIO> NAME_MAP = NameMap.createWithBaseTranslationKey(IO, "io_mode", values());

	private final String name;

	EnumIO(String n)
	{
		name = n;
	}

	@Override
	public String getName()
	{
		return name;
	}

	public Icon getIcon()
	{
		switch (this)
		{
			case IO:
				return GuiIcons.INV_IO;
			case IN:
				return GuiIcons.INV_IN;
			case OUT:
				return GuiIcons.INV_OUT;
			default:
				return GuiIcons.INV_NONE;
		}
	}

	public boolean canInsert()
	{
		return this == IO || this == IN;
	}

	public boolean canExtract()
	{
		return this == IO || this == OUT;
	}
}