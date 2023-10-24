package serverutils.lib.config;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import com.google.gson.JsonElement;

import serverutils.lib.gui.IOpenableGui;
import serverutils.lib.gui.misc.GuiEditConfigValue;
import serverutils.lib.icon.Color4I;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.io.DataReader;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.IWithID;
import serverutils.lib.util.JsonUtils;
import serverutils.lib.util.misc.MouseButton;

public abstract class ConfigValue implements IWithID {

    public abstract String getString();

    public abstract boolean getBoolean();

    public abstract int getInt();

    public double getDouble() {
        return getLong();
    }

    public long getLong() {
        return getInt();
    }

    public Ticks getTimer() {
        return Ticks.get(getLong());
    }

    public abstract ConfigValue copy();

    public boolean equalsValue(ConfigValue value) {
        return value == this || getString().equals(value.getString());
    }

    public Color4I getColor() {
        return Color4I.GRAY;
    }

    public void addInfo(ConfigValueInstance inst, List<String> list) {
        if (inst.getCanEdit() && !inst.getDefaultValue().isNull()) {
            list.add(
                    EnumChatFormatting.AQUA + "Default: "
                            + EnumChatFormatting.RESET
                            + inst.getDefaultValue().getStringForGUI().getFormattedText());
        }
    }

    public List<String> getVariants() {
        return Collections.emptyList();
    }

    public boolean isNull() {
        return false;
    }

    public void onClicked(IOpenableGui gui, ConfigValueInstance inst, MouseButton button, Runnable callback) {
        if (this instanceof IIteratingConfig) {
            if (inst.getCanEdit()) {
                setValueFromOtherValue(((IIteratingConfig) this).getIteration(button.isLeft()));
                callback.run();
            }

            return;
        }

        new GuiEditConfigValue(inst, (value, set) -> {
            if (set) {
                setValueFromOtherValue(value);
                callback.run();
            }

            gui.openGui();
        }).openGui();
    }

    public boolean setValueFromString(@Nullable ICommandSender sender, String string, boolean simulate) {
        JsonElement json = DataReader.get(string).safeJson();

        if (!json.isJsonNull()) {
            if (!simulate) {
                NBTTagCompound nbt = new NBTTagCompound();
                NBTBase nbt1 = JsonUtils.toNBT(json);

                if (nbt1 != null) {
                    nbt.setTag("x", nbt1);
                }

                readFromNBT(nbt, "x");
            }

            return true;
        }

        return false;
    }

    public void setValueFromOtherValue(ConfigValue value) {
        setValueFromString(null, value.getString(), false);
    }

    public void setValueFromJson(JsonElement json) {
        if (json.isJsonPrimitive()) {
            setValueFromString(null, json.getAsString(), false);
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ConfigValue value && equalsValue(value);
    }

    @Override
    public final String toString() {
        return getString();
    }

    public IChatComponent getStringForGUI() {
        return new ChatComponentText(getString());
    }

    public abstract void writeToNBT(NBTTagCompound nbt, String key);

    public abstract void readFromNBT(NBTTagCompound nbt, String key);

    public abstract void writeData(DataOut data);

    public abstract void readData(DataIn data);

    public boolean isEmpty() {
        return !getBoolean();
    }
}
