package serverutils.serverlib.lib.util.misc;

/**
 * @author LatvianModder
 */
public enum TimeType
{
	TICKS,
	MILLIS;

	public static final NameMap<TimeType> NAME_MAP = NameMap.create(TICKS, values());
}