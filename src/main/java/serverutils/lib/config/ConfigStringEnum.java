package serverutils.lib.config;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import serverutils.lib.gui.GuiBase;
import serverutils.lib.gui.GuiHelper;
import serverutils.lib.gui.IOpenableGui;
import serverutils.lib.gui.Panel;
import serverutils.lib.gui.SimpleTextButton;
import serverutils.lib.gui.misc.GuiButtonListBase;
import serverutils.lib.icon.Color4I;
import serverutils.lib.icon.Icon;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.math.MathUtils;
import serverutils.lib.util.misc.MouseButton;

public class ConfigStringEnum extends ConfigValue implements IIteratingConfig {

    public static final class StringEnumValue {

        public final int index;
        public final String id;
        public IChatComponent customName = null;
        public Color4I customColor = Icon.EMPTY;

        public StringEnumValue(int idx, String i) {
            index = idx;
            id = i;
        }

        public int hashCode() {
            return index;
        }

        public boolean equals(Object o) {
            return o == this || index == o.hashCode();
        }

        public String toString() {
            return id;
        }
    }

    private final List<StringEnumValue> values;
    private StringEnumValue value;

    public ConfigStringEnum(List<String> k, String v) {
        values = new ArrayList<>(k.size());

        for (int i = 0; i < k.size(); i++) {
            values.add(new StringEnumValue(i, k.get(i)));
        }

        value = get(v);
    }

    private ConfigStringEnum(ConfigStringEnum copyFrom) {
        values = new ArrayList<>(copyFrom.values.size());

        for (int i = 0; i < copyFrom.values.size(); i++) {
            StringEnumValue v0 = copyFrom.values.get(i);
            StringEnumValue v = new StringEnumValue(i, v0.id);
            v.customName = v0.customName == null ? null : v0.customName.createCopy();
            v.customColor = v0.customColor.copy();
            values.add(v);
        }

        setString(copyFrom.getString());
    }

    @Nullable
    public StringEnumValue get(String v) {
        if (v.isEmpty()) {
            return null;
        }

        for (StringEnumValue value : values) {
            if (value.id.equals(v)) {
                return value;
            }
        }

        return null;
    }

    @Override
    public String getId() {
        return ConfigEnum.ID;
    }

    public void setString(String v) {
        value = get(v);
    }

    @Override
    public String getString() {
        return value == null ? "" : value.id;
    }

    @Override
    public IChatComponent getStringForGUI() {
        return value == null ? new ChatComponentText("null")
                : value.customName == null ? new ChatComponentText(getString()) : value.customName.createCopy();
    }

    @Override
    public boolean getBoolean() {
        return true;
    }

    @Override
    public int getInt() {
        return value == null ? -1 : value.index;
    }

    @Override
    public ConfigStringEnum copy() {
        return new ConfigStringEnum(this);
    }

    @Override
    public Color4I getColor() {
        return value == null || value.customColor.isEmpty() ? ConfigEnum.COLOR : value.customColor;
    }

    @Override
    public void addInfo(ConfigValueInstance inst, List<String> list) {
        if (inst.getCanEdit() && !inst.getDefaultValue().isNull()) {
            StringEnumValue value = get(inst.getDefaultValue().getString());
            IChatComponent component = value == null ? null : value.customName;
            list.add(
                    EnumChatFormatting.AQUA + "Default: "
                            + EnumChatFormatting.RESET
                            + (component == null ? inst.getDefaultValue() : component.getFormattedText()));
        }

        list.add("");

        for (StringEnumValue v : values) {
            list.add(
                    (v == value ? (EnumChatFormatting.AQUA + "+ ") : (EnumChatFormatting.DARK_GRAY + "- "))
                            + (v.customName == null ? v.id : v.customName.getUnformattedText()));
        }
    }

    @Override
    public void onClicked(IOpenableGui gui, ConfigValueInstance inst, MouseButton button, Runnable callback) {
        if (values.size() > 16 || GuiBase.isCtrlKeyDown()) {
            GuiButtonListBase g = new GuiButtonListBase() {

                @Override
                public void addButtons(Panel panel) {
                    for (StringEnumValue v : values) {
                        panel.add(
                                new SimpleTextButton(
                                        panel,
                                        v.customName == null ? getString() : v.customName.getUnformattedText(),
                                        Icon.EMPTY) {

                                    @Override
                                    public void onClicked(MouseButton button) {
                                        GuiHelper.playClickSound();
                                        setString(v.id);
                                        gui.openGui();
                                        callback.run();
                                    }
                                });
                    }
                }
            };

            g.setHasSearchBox(true);
            g.openGui();
            return;
        }

        super.onClicked(gui, inst, button, callback);
    }

    @Override
    public List<String> getVariants() {
        List<String> l = new ArrayList<>(values.size());

        for (StringEnumValue v : values) {
            l.add(v.id);
        }

        return l;
    }

    @Override
    public ConfigValue getIteration(boolean next) {
        ConfigStringEnum c = copy();
        c.setString(
                value == null ? "" : c.values.get(MathUtils.mod(c.value.index + (next ? 1 : -1), c.values.size())).id);
        return c;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String key) {
        nbt.setString(key, getString());
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String key) {
        setString(nbt.getString(key));
    }

    @Override
    public void writeData(DataOut data) {
        data.writeVarInt(values.size());

        for (StringEnumValue v : values) {
            data.writeString(v.id);
            data.writeTextComponent(v.customName);
            data.writeInt(v.customColor.rgba());
        }

        data.writeString(getString());
    }

    @Override
    public void readData(DataIn data) {
        values.clear();
        int s = data.readVarInt();

        for (int i = 0; i < s; i++) {
            StringEnumValue v = new StringEnumValue(i, data.readString());
            v.customName = data.readTextComponent();
            v.customColor = Color4I.rgba(data.readInt());
            values.add(v);
        }

        setString(data.readString());
    }

    @Override
    public boolean setValueFromString(@Nullable ICommandSender sender, String string, boolean simulate) {
        setString(string);
        return true;
    }

    @Override
    public void setValueFromOtherValue(ConfigValue value) {
        setString(value.getString());
    }
}
