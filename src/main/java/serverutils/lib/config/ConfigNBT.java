package serverutils.lib.config;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import com.google.gson.JsonElement;

import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.util.JsonUtils;
import serverutils.lib.util.NBTUtils;

public class ConfigNBT extends ConfigValue {

    public static final String ID = "nbt";

    private NBTTagCompound value;

    public ConfigNBT(@Nullable NBTTagCompound nbt) {
        value = nbt;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getString() {
        return value == null ? "null" : value.toString();
    }

    @Nullable
    public NBTTagCompound getNBT() {
        return value;
    }

    public void setNBT(@Nullable NBTTagCompound nbt) {
        value = nbt;
    }

    @Override
    public boolean getBoolean() {
        value = getNBT();
        return value != null && !value.hasNoTags();
    }

    @Override
    public int getInt() {
        value = getNBT();
        return value == null ? 0 : value.hashCode();
    }

    @Override
    public ConfigNBT copy() {
        value = getNBT();
        return new ConfigNBT(value == null ? null : (NBTTagCompound) value.copy());
    }

    @Override
    public IChatComponent getStringForGUI() {
        return new ChatComponentText(getNBT() == null ? "null" : "{...}");
    }

    @Override
    public boolean setValueFromString(@Nullable ICommandSender sender, String string, boolean simulate) {
        if (string.equals("null")) {
            if (!simulate) {
                setNBT(null);
            }

            return true;
        }

        try {
            value = (NBTTagCompound) JsonToNBT.func_150315_a(string);

            if (!simulate) {
                setNBT(value);
            }

            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public void addInfo(ConfigValueInstance inst, List<String> list) {
        list.add(
                EnumChatFormatting.AQUA + "Value: "
                        + EnumChatFormatting.RESET
                        + NBTUtils.getColoredNBTString(getNBT()));

        if (inst.getCanEdit() && inst.getDefaultValue() instanceof ConfigNBT) {
            list.add(
                    EnumChatFormatting.AQUA + "Default: "
                            + EnumChatFormatting.RESET
                            + NBTUtils.getColoredNBTString(((ConfigNBT) inst.getDefaultValue()).getNBT()));
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String key) {
        value = getNBT();

        if (value != null) {
            nbt.setTag(key, value);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String key) {
        value = nbt.hasKey(key) ? nbt.getCompoundTag(key) : null;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeNBT(getNBT());
    }

    @Override
    public void readData(DataIn data) {
        setNBT(data.readNBT());
    }

    @Override
    public boolean isEmpty() {
        value = getNBT();
        return value == null || value.hasNoTags();
    }

    @Override
    public void setValueFromOtherValue(ConfigValue value) {
        if (value instanceof ConfigNBT) {
            NBTTagCompound nbt = ((ConfigNBT) value).getNBT();
            setNBT(nbt == null ? null : (NBTTagCompound) nbt.copy());
        } else {
            super.setValueFromOtherValue(value);
        }
    }

    @Override
    public void setValueFromJson(JsonElement json) {
        if (json.isJsonObject()) {
            setNBT((NBTTagCompound) JsonUtils.toNBT(json));
        }
    }
}
