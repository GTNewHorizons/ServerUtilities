package serverutils.lib.config;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import javax.annotation.Nullable;

import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;

import com.google.gson.JsonElement;

import serverutils.lib.icon.Color4I;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;

public class ConfigInt extends ConfigValue implements IntSupplier {

    public static final String ID = "int";
    public static final Color4I COLOR = Color4I.rgb(0xAA5AE8);

    public static class SimpleInt extends ConfigInt {

        private final IntSupplier get;
        private final IntConsumer set;

        public SimpleInt(int min, int max, IntSupplier g, IntConsumer s) {
            super(0, min, max);
            get = g;
            set = s;
        }

        @Override
        public int getInt() {
            return get.getAsInt();
        }

        @Override
        public void setInt(int v) {
            set.accept(v);
        }
    }

    private int value;
    private int min = Integer.MIN_VALUE;
    private int max = Integer.MAX_VALUE;

    public ConfigInt() {}

    public ConfigInt(int v) {
        value = v;
    }

    public ConfigInt(int v, int mn, int mx) {
        this(MathHelper.clamp_int(v, mn, mx));
        min = mn;
        max = mx;
    }

    @Override
    public String getId() {
        return ID;
    }

    public ConfigInt setMin(int v) {
        min = v;
        return this;
    }

    public ConfigInt setMax(int v) {
        max = v;
        return this;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public void setInt(int v) {
        value = MathHelper.clamp_int(v, getMin(), getMax());
    }

    @Override
    public String getString() {
        return Integer.toString(getInt());
    }

    @Override
    public boolean getBoolean() {
        return getInt() != 0;
    }

    @Override
    public int getInt() {
        return value;
    }

    @Override
    public ConfigInt copy() {
        return new ConfigInt(getInt(), getMin(), getMax());
    }

    @Override
    public boolean equalsValue(ConfigValue value) {
        return getInt() == value.getInt();
    }

    @Override
    public Color4I getColor() {
        return COLOR;
    }

    @Override
    public void addInfo(ConfigValueInstance inst, List<String> list) {
        super.addInfo(inst, list);

        int m = getMin();

        if (m != Integer.MIN_VALUE) {
            list.add(EnumChatFormatting.AQUA + "Min: " + EnumChatFormatting.RESET + m);
        }

        m = getMax();

        if (m != Integer.MAX_VALUE) {
            list.add(EnumChatFormatting.AQUA + "Max: " + EnumChatFormatting.RESET + m);
        }
    }

    @Override
    public boolean setValueFromString(@Nullable ICommandSender sender, String string, boolean simulate) {
        try {
            int val;
            if (string.startsWith("#") || string.length() >= 10) {
                val = Long.decode(string).intValue();
            } else {
                val = Integer.parseInt(string);
            }

            if (!simulate) {
                setInt(val);
            }

            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String key) {
        value = getInt();

        if (value != 0) {
            nbt.setDouble(key, value);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String key) {
        setInt(nbt.getInteger(key));
    }

    @Override
    public void writeData(DataOut data) {
        data.writeVarInt(getInt());
        data.writeVarInt(getMin());
        data.writeVarInt(getMax());
    }

    @Override
    public void readData(DataIn data) {
        setInt(data.readVarInt());
        setMin(data.readVarInt());
        setMax(data.readVarInt());
    }

    @Override
    public int getAsInt() {
        return getInt();
    }

    @Override
    public void setValueFromOtherValue(ConfigValue value) {
        setInt(value.getInt());
    }

    @Override
    public void setValueFromJson(JsonElement json) {
        if (json.isJsonPrimitive()) {
            setInt(json.getAsInt());
        }
    }
}
