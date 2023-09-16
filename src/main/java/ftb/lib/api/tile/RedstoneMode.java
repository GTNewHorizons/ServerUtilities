package ftb.lib.api.tile;

import ftb.lib.api.LangKey;

public enum RedstoneMode {

    DISABLED("disabled"),
    ACTIVE_HIGH("high"),
    ACTIVE_LOW("low");

    public static final RedstoneMode[] VALUES = values();
    public static final String enumLangKey = "ftbl.redstonemode";

    public final int ID;
    public final LangKey lang;

    RedstoneMode(String s) {
        ID = ordinal();
        lang = new LangKey(enumLangKey + '.' + s);
    }

    public boolean cancel(boolean b) {
        if (this == DISABLED) return false;
        if (this == ACTIVE_HIGH && !b) return true;
        if (this == ACTIVE_LOW && b) return true;
        return false;
    }

    public RedstoneMode next() {
        return VALUES[(ID + 1) % VALUES.length];
    }

    public RedstoneMode prev() {
        int id = ID - 1;
        if (id < 0) id = VALUES.length - 1;
        return VALUES[id];
    }
}
