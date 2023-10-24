package serverutils.lib.util.misc;

import serverutils.lib.gui.GuiIcons;
import serverutils.lib.icon.Icon;
import serverutils.lib.util.IStringSerializable;

public enum EnumRedstoneMode implements IStringSerializable {

    DISABLED("disabled"),
    ACTIVE_HIGH("active_high"),
    ACTIVE_LOW("active_low"),
    PULSE("pulse");

    public static final NameMap<EnumRedstoneMode> NAME_MAP = NameMap
            .createWithBaseTranslationKey(DISABLED, "redstone_mode", DISABLED, ACTIVE_HIGH, ACTIVE_LOW);
    public static final NameMap<EnumRedstoneMode> NAME_MAP_WITH_PULSE = NameMap
            .createWithBaseTranslationKey(DISABLED, "redstone_mode", DISABLED, ACTIVE_HIGH, ACTIVE_LOW, PULSE);

    private final String name;

    EnumRedstoneMode(String n) {
        name = n;
    }

    @Override
    public String getName() {
        return name;
    }

    public boolean isActive(boolean prevValue, boolean value) {
        return switch (this) {
            case ACTIVE_HIGH -> value;
            case ACTIVE_LOW -> !value;
            default -> false;
        };
    }

    public Icon getIcon() {
        return switch (this) {
            case ACTIVE_HIGH -> GuiIcons.RS_HIGH;
            case ACTIVE_LOW -> GuiIcons.RS_LOW;
            case PULSE -> GuiIcons.RS_PULSE;
            default -> GuiIcons.RS_NONE;
        };
    }
}
