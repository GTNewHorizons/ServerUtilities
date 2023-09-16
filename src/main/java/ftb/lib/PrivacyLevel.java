package ftb.lib;

import ftb.lib.api.LangKey;
import ftb.lib.api.gui.GuiIcons;

public enum PrivacyLevel {

    PUBLIC,
    PRIVATE,
    FRIENDS;

    public static final PrivacyLevel[] VALUES_3 = new PrivacyLevel[] { PUBLIC, PRIVATE, FRIENDS };
    public static final PrivacyLevel[] VALUES_2 = new PrivacyLevel[] { PUBLIC, PRIVATE };
    public static final String enumLangKey = "ftbl.security";

    public final int ID;
    public final LangKey lang;

    PrivacyLevel() {
        ID = ordinal();
        lang = new LangKey(enumLangKey + '.' + name().toLowerCase());
    }

    public boolean isPublic() {
        return this == PUBLIC;
    }

    public boolean isRestricted() {
        return this == FRIENDS;
    }

    public PrivacyLevel next(PrivacyLevel[] l) {
        return l[(ID + 1) % l.length];
    }

    public PrivacyLevel prev(PrivacyLevel[] l) {
        int id = ID - 1;
        if (id < 0) id = l.length - 1;
        return l[id];
    }

    public TextureCoords getIcon() {
        return GuiIcons.security[ID];
    }

    public static String[] getNames() {
        String[] s = new String[VALUES_3.length];
        for (int i = 0; i < VALUES_3.length; i++) s[i] = VALUES_3[i].name().toLowerCase();
        return s;
    }

    public static PrivacyLevel get(String s) {
        for (PrivacyLevel l : VALUES_3) {
            if (l.name().equalsIgnoreCase(s)) return l;
        }
        return null;
    }
}
