package serverutils.lib.util.misc;

import serverutils.lib.gui.GuiIcons;
import serverutils.lib.icon.Icon;
import serverutils.lib.util.IStringSerializable;

public enum EnumIO implements IStringSerializable {

    IO("io"),
    IN("in"),
    OUT("out"),
    NONE("none");

    public static final NameMap<EnumIO> NAME_MAP = NameMap.createWithBaseTranslationKey(IO, "io_mode", values());

    private final String name;

    EnumIO(String n) {
        name = n;
    }

    @Override
    public String getName() {
        return name;
    }

    public Icon getIcon() {
        return switch (this) {
            case IO -> GuiIcons.INV_IO;
            case IN -> GuiIcons.INV_IN;
            case OUT -> GuiIcons.INV_OUT;
            default -> GuiIcons.INV_NONE;
        };
    }

    public boolean canInsert() {
        return this == IO || this == IN;
    }

    public boolean canExtract() {
        return this == IO || this == OUT;
    }
}
