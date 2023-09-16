package ftb.lib.api.tile;

import ftb.lib.api.LangKey;

public enum InvMode {

    ENABLED("enabled"),
    ONLY_IN("onlyin"),
    ONLY_OUT("onlyout"),
    DISABLED("disabled");

    public static final InvMode[] VALUES = values();
    public static final String enumLangKey = "ftbl.invmode";

    public final int ID;
    public final LangKey lang;

    InvMode(String s) {
        ID = ordinal();
        lang = new LangKey(enumLangKey + '.' + s);
    }

    public InvMode next() {
        return VALUES[(ID + 1) % VALUES.length];
    }

    public InvMode prev() {
        int id = ID - 1;
        if (id < 0) id = VALUES.length - 1;
        return VALUES[id];
    }

    public boolean canInsertItem() {
        return this == ENABLED || this == ONLY_IN;
    }

    public boolean canExtractItem() {
        return this == ENABLED || this == ONLY_OUT;
    }
}
