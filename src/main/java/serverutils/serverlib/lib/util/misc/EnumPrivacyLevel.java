package serverutils.serverlib.lib.util.misc;

import serverutils.serverlib.lib.gui.GuiIcons;
import serverutils.serverlib.lib.icon.Icon;
import net.minecraft.util.IStringSerializable;

/**
 * @author LatvianModder
 */
public enum EnumPrivacyLevel implements IStringSerializable
{
	PUBLIC("public"),
	PRIVATE("private"),
	TEAM("team");

	public static final EnumPrivacyLevel[] VALUES = values();
	public static final NameMap<EnumPrivacyLevel> NAME_MAP = NameMap.createWithBaseTranslationKey(PUBLIC, "ftblib.privacy", VALUES);

	private final String name;

	EnumPrivacyLevel(String n)
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
			case PRIVATE:
				return GuiIcons.SECURITY_PRIVATE;
			case TEAM:
				return GuiIcons.SECURITY_TEAM;
			default:
				return GuiIcons.SECURITY_PUBLIC;
		}
	}

	public boolean isPublic()
	{
		return this == PUBLIC;
	}
}