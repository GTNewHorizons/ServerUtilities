package serverutils.lib.tile;

public enum EnumSaveType {

    SAVE(true, true, false),
    NET_FULL(false, true, false),
    NET_UPDATE(false, false, false),
    ITEM(true, true, true);

    public final boolean save;
    public final boolean full;
    public final boolean item;

    EnumSaveType(boolean s, boolean f, boolean i) {
        save = s;
        full = f;
        item = i;
    }
}
