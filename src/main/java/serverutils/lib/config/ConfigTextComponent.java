package serverutils.lib.config;

import javax.annotation.Nullable;

import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IChatComponent;

import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.io.DataReader;
import serverutils.lib.util.JsonUtils;

public class ConfigTextComponent extends ConfigValue {

    public static final String ID = "text_component";

    private IChatComponent value;

    public ConfigTextComponent(IChatComponent c) {
        value = c;
    }

    @Override
    public String getId() {
        return ID;
    }

    public IChatComponent getText() {
        return value;
    }

    public void setText(IChatComponent c) {
        value = c;
    }

    @Override
    public String getString() {
        return JsonUtils.serializeTextComponent(getText()).toString();
    }

    @Override
    public boolean setValueFromString(@Nullable ICommandSender sender, String string, boolean simulate) {
        IChatComponent component = JsonUtils.deserializeTextComponent(DataReader.get(string).safeJson());

        if (component != null) {
            if (!simulate) {
                setText(component);
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean getBoolean() {
        return !getString().isEmpty();
    }

    @Override
    public int getInt() {
        return getString().length();
    }

    @Override
    public ConfigTextComponent copy() {
        return new ConfigTextComponent(getText());
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String key) {
        nbt.setTag(key, JsonUtils.toNBT(JsonUtils.serializeTextComponent(getText())));
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String key) {
        setText(JsonUtils.deserializeTextComponent(JsonUtils.toJson(nbt.getTag(key))));
    }

    @Override
    public void writeData(DataOut data) {
        data.writeTextComponent(getText());
    }

    @Override
    public void readData(DataIn data) {
        setText(data.readTextComponent());
    }

    @Override
    public IChatComponent getStringForGUI() {
        return getText().createCopy();
    }

    @Override
    public void setValueFromOtherValue(ConfigValue value) {
        if (value instanceof ConfigTextComponent) {
            setText(((ConfigTextComponent) value).getText().createCopy());
        } else {
            super.setValueFromOtherValue(value);
        }
    }

    @Override
    public boolean isEmpty() {
        return getText().getUnformattedText().isEmpty();
    }
}
