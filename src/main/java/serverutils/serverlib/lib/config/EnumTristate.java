package serverutils.serverlib.lib.config;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IStringSerializable;
import net.minecraftforge.fml.common.eventhandler.Event;
import serverutils.serverlib.lib.icon.Color4I;
import serverutils.serverlib.lib.util.misc.NameMap;

public enum EnumTristate implements IStringSerializable
{
	TRUE("true", Event.Result.ALLOW, ConfigBoolean.COLOR_TRUE, 1),
	FALSE("false", Event.Result.DENY, ConfigBoolean.COLOR_FALSE, 0),
	DEFAULT("default", Event.Result.DEFAULT, ConfigEnum.COLOR, 2);

	public static final NameMap<EnumTristate> NAME_MAP = NameMap.createWithNameAndColor(DEFAULT, (sender, value) -> new ChatComponentTranslation(value.getName()), EnumTristate::getColor, values());

	public static EnumTristate read(NBTTagCompound nbt, String key)
	{
		return nbt.hasKey(key) ? nbt.getBoolean(key) ? TRUE : FALSE : DEFAULT;
	}

	private final String name;
	private final Event.Result result;
	private final Color4I color;
	private final int opposite;

	EnumTristate(String s, Event.Result r, Color4I c, int o)
	{
		name = s;
		result = r;
		color = c;
		opposite = o;
	}

	@Override
	public String getName()
	{
		return name;
	}

	public Event.Result getResult()
	{
		return result;
	}

	public Color4I getColor()
	{
		return color;
	}

	public boolean isTrue()
	{
		return this == TRUE;
	}

	public boolean isFalse()
	{
		return this == FALSE;
	}

	public boolean isDefault()
	{
		return this == DEFAULT;
	}

	public boolean get(boolean def)
	{
		return isDefault() ? def : isTrue();
	}

	public EnumTristate getOpposite()
	{
		return NAME_MAP.get(opposite);
	}

	public String toString()
	{
		return name;
	}

	public void write(NBTTagCompound nbt, String key)
	{
		if (!isDefault())
		{
			nbt.setBoolean(key, isTrue());
		}
	}
}