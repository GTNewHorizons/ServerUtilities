package serverutils.lib.util.misc;

public enum TimeType {

    TICKS,
    MILLIS;

    public static final NameMap<TimeType> NAME_MAP = NameMap.create(TICKS, values());
}
