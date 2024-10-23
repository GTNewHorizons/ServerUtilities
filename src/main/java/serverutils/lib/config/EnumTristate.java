package serverutils.lib.config;

import serverutils.lib.util.IStringSerializable;

public enum EnumTristate implements IStringSerializable {

    TRUE,
    FALSE,
    DEFAULT;

    @Override
    public String getName() {
        return name().toLowerCase();
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
}
