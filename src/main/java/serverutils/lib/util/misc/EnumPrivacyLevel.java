package serverutils.lib.util.misc;

import serverutils.lib.gui.GuiIcons;
import serverutils.lib.icon.Icon;
import serverutils.lib.util.IStringSerializable;

public enum EnumPrivacyLevel implements IStringSerializable {

    PUBLIC("public"),
    PRIVATE("private"),
    TEAM("team");

    public static final EnumPrivacyLevel[] VALUES = values();
    public static final NameMap<EnumPrivacyLevel> NAME_MAP = NameMap
            .createWithBaseTranslationKey(PUBLIC, "serverutilities.privacy", VALUES);

    private final String name;

    EnumPrivacyLevel(String n) {
        name = n;
    }

    @Override
    public String getName() {
        return name;
    }

    public Icon getIcon() {
        return switch (this) {
            case PRIVATE -> GuiIcons.SECURITY_PRIVATE;
            case TEAM -> GuiIcons.SECURITY_TEAM;
            default -> GuiIcons.SECURITY_PUBLIC;
        };
    }

    public boolean isPublic() {
        return this == PUBLIC;
    }
}
