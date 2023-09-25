package serverutils.lib.lib.config;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;

import serverutils.lib.lib.icon.Color4I;
import serverutils.lib.lib.util.IStringSerializable;
import serverutils.lib.lib.util.misc.NameMap;

import cpw.mods.fml.common.eventhandler.Event;

public enum EnumTristate implements IStringSerializable {

	TRUE("true", Event.Result.ALLOW, ConfigBoolean.COLOR_TRUE, 1),
	FALSE("false", Event.Result.DENY, ConfigBoolean.COLOR_FALSE, 0),
	DEFAULT("default", Event.Result.DEFAULT, ConfigEnum.COLOR, 2);

	public static final NameMap<EnumTristate> NAME_MAP = NameMap.createWithNameAndColor(
			DEFAULT,
			(sender, value) -> new ChatComponentTranslation(value.getName()),
			EnumTristate::getColor,
			values());

	public static EnumTristate read(NBTTagCompound nbt, String key) {
		return nbt.hasKey(key) ? nbt.getBoolean(key) ? TRUE : FALSE : DEFAULT;
	}

	public static EnumTristate string2tristate(String tristate) {
		switch (tristate) {
			case "true":
				return TRUE;
			case "false":
				return FALSE;
			default:
				return DEFAULT;
		}
	}

	private final String name;
	private final Event.Result result;
	private final Color4I color;
	private final int opposite;

	EnumTristate(String s, Event.Result r, Color4I c, int o) {
		name = s;
		result = r;
		color = c;
		opposite = o;
	}

	@Override
	public String getName() {
		return name;
	}

	public Event.Result getResult() {
		return result;
	}

	public Color4I getColor() {
		return color;
	}

	public boolean isTrue() {
		return this == TRUE;
	}

	public boolean isFalse() {
		return this == FALSE;
	}

	public boolean isDefault() {
		return this == DEFAULT;
	}

	public boolean get(boolean def) {
		return isDefault() ? def : isTrue();
	}

	public EnumTristate getOpposite() {
		return NAME_MAP.get(opposite);
	}

	public String toString() {
		return name;
	}

	public void write(NBTTagCompound nbt, String key) {
		if (!isDefault()) {
			nbt.setBoolean(key, isTrue());
		}
	}
}