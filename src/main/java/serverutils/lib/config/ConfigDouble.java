package serverutils.lib.config;

import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

import javax.annotation.Nullable;

import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;

import com.google.gson.JsonElement;

import serverutils.lib.icon.Color4I;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.util.StringUtils;

public class ConfigDouble extends ConfigValue implements DoubleSupplier {

    public static final String ID = "double";

    public static class SimpleDouble extends ConfigDouble {

        private final DoubleSupplier get;
        private final DoubleConsumer set;

        public SimpleDouble(double min, double max, DoubleSupplier g, DoubleConsumer s) {
            super(0D, min, max);
            get = g;
            set = s;
        }

        @Override
        public double getDouble() {
            return get.getAsDouble();
        }

        @Override
        public void setDouble(double v) {
            set.accept(v);
        }
    }

    private double value;
    private double min = Double.NEGATIVE_INFINITY;
    private double max = Double.POSITIVE_INFINITY;

    public ConfigDouble(double v) {
        value = v;
    }

    public ConfigDouble(double v, double mn, double mx) {
        this(MathHelper.clamp_double(v, mn, mx));
        min = mn;
        max = mx;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public double getDouble() {
        return value;
    }

    public void setDouble(double v) {
        value = MathHelper.clamp_double(v, getMin(), getMax());
    }

    public ConfigDouble setMin(double v) {
        min = v;
        return this;
    }

    public ConfigDouble setMax(double v) {
        max = v;
        return this;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    @Override
    public String getString() {
        String s = Double.toString(getDouble());
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }

    @Override
    public IChatComponent getStringForGUI() {
        return new ChatComponentText(StringUtils.formatDouble(getDouble(), true));
    }

    @Override
    public boolean getBoolean() {
        return getDouble() != 0D;
    }

    @Override
    public int getInt() {
        return (int) getDouble();
    }

    @Override
    public long getLong() {
        return (long) getDouble();
    }

    @Override
    public ConfigDouble copy() {
        return new ConfigDouble(getDouble(), getMin(), getMax());
    }

    @Override
    public boolean equalsValue(ConfigValue value) {
        return getDouble() == value.getDouble();
    }

    @Override
    public Color4I getColor() {
        return ConfigInt.COLOR;
    }

    @Override
    public void addInfo(ConfigValueInstance inst, List<String> list) {
        super.addInfo(inst, list);
        double min = getMin();
        double max = getMax();

        if (min != Double.NEGATIVE_INFINITY) {
            list.add(EnumChatFormatting.AQUA + "Min: " + EnumChatFormatting.RESET + StringUtils.formatDouble(min));
        }

        if (max != Double.POSITIVE_INFINITY) {
            list.add(EnumChatFormatting.AQUA + "Max: " + EnumChatFormatting.RESET + StringUtils.formatDouble(max));
        }
    }

    @Override
    public boolean setValueFromString(@Nullable ICommandSender sender, String string, boolean simulate) {
        if (string.isEmpty()) {
            return false;
        }

        double min = getMin();
        double max = getMax();

        if (string.equals("+Inf")) {
            if (max != Double.POSITIVE_INFINITY) {
                return false;
            }

            if (!simulate) {
                setDouble(Double.POSITIVE_INFINITY);
            }

            return true;
        } else if (string.equals("-Inf")) {
            if (min != Double.NEGATIVE_INFINITY) {
                return false;
            }

            if (!simulate) {
                setDouble(Double.NEGATIVE_INFINITY);
            }

            return true;
        }

        try {
            double multiplier = 1D;

            if (string.endsWith("K")) {
                multiplier = 1000D;
                string = string.substring(0, string.length() - 1);
            } else if (string.endsWith("M")) {
                multiplier = 1000000D;
                string = string.substring(0, string.length() - 1);
            } else if (string.endsWith("B")) {
                multiplier = 1000000000D;
                string = string.substring(0, string.length() - 1);
            }

            double val = Double.parseDouble(string.trim()) * multiplier;

            if (val < min || val > max) {
                return false;
            }

            if (!simulate) {
                setDouble(val);
            }

            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String key) {
        value = getDouble();

        if (value != 0D) {
            nbt.setDouble(key, value);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String key) {
        setDouble(nbt.getDouble(key));
    }

    @Override
    public void writeData(DataOut data) {
        data.writeDouble(getDouble());
        data.writeDouble(getMin());
        data.writeDouble(getMax());
    }

    @Override
    public void readData(DataIn data) {
        setDouble(data.readDouble());
        setMin(data.readDouble());
        setMax(data.readDouble());
    }

    @Override
    public double getAsDouble() {
        return getDouble();
    }

    @Override
    public void setValueFromOtherValue(ConfigValue value) {
        setDouble(value.getDouble());
    }

    @Override
    public void setValueFromJson(JsonElement json) {
        if (json.isJsonPrimitive()) {
            setDouble(json.getAsDouble());
        }
    }
}
