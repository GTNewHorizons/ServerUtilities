package serverutils.lib.config;

import java.util.List;
import java.util.function.LongSupplier;

import javax.annotation.Nullable;

import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;

import serverutils.lib.icon.Color4I;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.math.Ticks;

public class ConfigTimer extends ConfigValue implements LongSupplier {

    public static final String ID = "timer";

    private Ticks value;
    private Ticks maxValue = Ticks.NO_TICKS;

    public ConfigTimer(Ticks v) {
        value = v;
    }

    public ConfigTimer(Ticks v, Ticks max) {
        this(v);
        maxValue = max;
    }

    @Override
    public String getId() {
        return ID;
    }

    public ConfigTimer setMax(Ticks v) {
        maxValue = v;
        return this;
    }

    public Ticks getMax() {
        return maxValue;
    }

    @Override
    public Ticks getTimer() {
        return value;
    }

    public void setTimer(Ticks v) {
        Ticks max = getMax();
        value = max.hasTicks() && v.ticks() >= max.ticks() ? max : v;
    }

    @Override
    public String getString() {
        return getTimer().toString();
    }

    @Override
    public boolean getBoolean() {
        return getTimer().hasTicks();
    }

    @Override
    public int getInt() {
        return (int) getTimer().ticks();
    }

    @Override
    public long getLong() {
        return getTimer().ticks();
    }

    @Override
    public ConfigTimer copy() {
        return new ConfigTimer(getTimer(), getMax());
    }

    @Override
    public boolean equalsValue(ConfigValue value) {
        return value instanceof ConfigTimer && getTimer().equalsTimer(value.getTimer());
    }

    @Override
    public Color4I getColor() {
        return ConfigInt.COLOR;
    }

    @Override
    public void addInfo(ConfigValueInstance inst, List<String> list) {
        super.addInfo(inst, list);

        Ticks max = getMax();

        if (max.hasTicks()) {
            list.add(EnumChatFormatting.AQUA + "Max: " + EnumChatFormatting.RESET + max);
        }
    }

    @Override
    public boolean setValueFromString(@Nullable ICommandSender sender, String string, boolean simulate) {
        if (string.length() > 2 && string.startsWith("\"") && string.endsWith("\"")) {
            string = string.substring(1, string.length() - 1);
        }

        if (string.isEmpty()) {
            return false;
        }

        try {
            value = Ticks.get(string);

            if (!simulate) {
                setTimer(value);
            }

            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String key) {
        nbt.setString(key, getTimer().toString());
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String key) {
        setTimer(Ticks.NO_TICKS);
        setValueFromString(null, nbt.getString(key), false);
    }

    @Override
    public void writeData(DataOut data) {
        data.writeVarLong(getTimer().ticks());
        data.writeVarLong(getMax().ticks());
    }

    @Override
    public void readData(DataIn data) {
        setTimer(Ticks.get(data.readVarLong()));
        setMax(Ticks.get(data.readVarLong()));
    }

    @Override
    public long getAsLong() {
        return getTimer().ticks();
    }

    @Override
    public void setValueFromOtherValue(ConfigValue value) {
        setTimer(value.getTimer());
    }
}
