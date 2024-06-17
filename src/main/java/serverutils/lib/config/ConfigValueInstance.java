package serverutils.lib.config;

import javax.annotation.Nullable;

import net.minecraft.util.IChatComponent;

import serverutils.lib.data.ServerUtilitiesAPI;
import serverutils.lib.gui.GuiIcons;
import serverutils.lib.icon.Icon;
import serverutils.lib.io.Bits;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.util.FinalIDObject;
import serverutils.lib.util.StringUtils;

public final class ConfigValueInstance extends FinalIDObject {

    private static final int HAS_NAME = 1;
    private static final int HIDDEN = 2;
    private static final int CANT_EDIT = 4;
    private static final int USE_SCROLL_BAR = 8;
    private static final int EXCLUDED = 16;
    private static final int HAS_INFO = 32;

    private final ConfigGroup group;
    private final ConfigValue value;
    private ConfigValue defaultValue;
    private int flags;
    private IChatComponent displayName, info;
    private int order;
    private Icon icon;

    public ConfigValueInstance(String id, ConfigGroup g, ConfigValue v, int flag) {
        super(id, flag);
        group = g;
        value = v;
        defaultValue = ConfigNull.INSTANCE;
        flags = 0;
        displayName = null;
        info = null;
        order = 0;
        icon = GuiIcons.SETTINGS_RED;
    }

    public ConfigValueInstance(String id, ConfigGroup g, ConfigValue v) {
        this(id, g, v, StringUtils.FLAG_ID_DEFAULTS);
    }

    public ConfigValueInstance(ConfigGroup g, DataIn data) {
        super(data.readString(), data.readVarInt());
        group = g;
        value = ServerUtilitiesAPI.createConfigValueFromId(data.readString());
        value.readData(data);
        defaultValue = ServerUtilitiesAPI.createConfigValueFromId(data.readString());
        defaultValue.readData(data);
        flags = data.readVarInt();
        order = data.readVarInt();
        displayName = Bits.getFlag(flags, HAS_NAME) ? data.readTextComponent() : null;
        info = Bits.getFlag(flags, HAS_INFO) ? data.readTextComponent() : null;
    }

    public ConfigGroup getGroup() {
        return group;
    }

    public ConfigValue getValue() {
        return value;
    }

    public ConfigValueInstance setDefaultValue(ConfigValue def) {
        if (def.isNull()) {
            defaultValue = ConfigNull.INSTANCE;
        } else {
            defaultValue = value.copy();
            defaultValue.setValueFromOtherValue(def);
        }

        return this;
    }

    public ConfigValue getDefaultValue() {
        return defaultValue;
    }

    public ConfigValueInstance setDisplayName(@Nullable IChatComponent name) {
        displayName = name;
        flags = Bits.setFlag(flags, HAS_NAME, displayName != null);
        return this;
    }

    public IChatComponent getDisplayName() {
        return displayName == null ? group.getDisplayNameOf(this) : displayName;
    }

    public ConfigValueInstance setInfo(@Nullable IChatComponent component) {
        info = component;
        flags = Bits.setFlag(flags, HAS_INFO, info != null);
        return this;
    }

    public IChatComponent getInfo() {
        return info == null ? group.getInfoOf(this) : info;
    }

    public ConfigValueInstance setHidden(boolean v) {
        flags = Bits.setFlag(flags, HIDDEN, v);
        return this;
    }

    public boolean getHidden() {
        return Bits.getFlag(flags, HIDDEN);
    }

    public ConfigValueInstance setCanEdit(boolean v) {
        flags = Bits.setFlag(flags, CANT_EDIT, !v);
        return this;
    }

    public boolean getCanEdit() {
        return !Bits.getFlag(flags, CANT_EDIT);
    }

    public ConfigValueInstance setUseScrollBar(boolean v) {
        flags = Bits.setFlag(flags, USE_SCROLL_BAR, v);
        return this;
    }

    public boolean getUseScrollBar() {
        return Bits.getFlag(flags, USE_SCROLL_BAR);
    }

    public ConfigValueInstance setExcluded(boolean v) {
        flags = Bits.setFlag(flags, EXCLUDED, v);
        return this;
    }

    public boolean getExcluded() {
        return Bits.getFlag(flags, EXCLUDED);
    }

    public ConfigValueInstance setOrder(int o) {
        order = o;
        return this;
    }

    public int getOrder() {
        return order;
    }

    public ConfigValueInstance setIcon(Icon i) {
        icon = i;
        return this;
    }

    public Icon getIcon() {
        return icon;
    }

    public void writeData(DataOut data) {
        data.writeString(value.getId());
        value.writeData(data);
        data.writeString(defaultValue.getId());
        defaultValue.writeData(data);
        data.writeVarInt(flags);
        data.writeVarInt(order);

        if (displayName != null) {
            data.writeTextComponent(displayName);
        }

        if (info != null) {
            data.writeTextComponent(info);
        }
    }

    public ConfigValueInstance copy(ConfigGroup g) {
        ConfigValueInstance inst = new ConfigValueInstance(getId(), g, value.copy(), getIdFlag());
        inst.defaultValue = defaultValue.copy();
        inst.displayName = displayName == null ? null : displayName.createCopy();
        inst.info = info == null ? null : info.createCopy();
        inst.flags = flags;
        inst.order = order;
        inst.icon = (icon == null || icon.isEmpty()) ? Icon.EMPTY : icon.copy();
        return inst;
    }

    public String getPath() {
        return group.getPath() + "." + getId();
    }
}
