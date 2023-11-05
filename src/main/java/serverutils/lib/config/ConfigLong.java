package serverutils.lib.config;

import java.util.List;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

import javax.annotation.Nullable;

import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import com.google.gson.JsonElement;

import serverutils.lib.icon.Color4I;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.util.StringUtils;

public class ConfigLong extends ConfigValue implements LongSupplier {

    public static final String ID = "long";

    public static class SimpleLong extends ConfigLong {

        private final LongSupplier get;
        private final LongConsumer set;

        public SimpleLong(long min, long max, LongSupplier g, LongConsumer s) {
            super(0L, min, max);
            get = g;
            set = s;
        }

        @Override
        public long getLong() {
            return get.getAsLong();
        }

        @Override
        public void setLong(long v) {
            set.accept(v);
        }
    }

    private long value;
    private long min = Long.MIN_VALUE;
    private long max = Long.MAX_VALUE;

    public ConfigLong(long v) {
        value = v;
    }

    public ConfigLong(long v, long mn, long mx) {
        this(Math.min(Math.max(v, mn), mx));
        min = mn;
        max = mx;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public long getLong() {
        return value;
    }

    public void setLong(long v) {
        value = v;
    }

    public ConfigLong setMin(long v) {
        min = v;
        return this;
    }

    public ConfigLong setMax(long v) {
        max = v;
        return this;
    }

    public long getMin() {
        return min;
    }

    public long getMax() {
        return max;
    }

    @Override
    public IChatComponent getStringForGUI() {
        return new ChatComponentText(StringUtils.formatDouble(getLong(), true));
    }

    @Override
    public String getString() {
        return Long.toString(getLong());
    }

    @Override
    public boolean getBoolean() {
        return getLong() != 0D;
    }

    @Override
    public int getInt() {
        return (int) getLong();
    }

    @Override
    public ConfigLong copy() {
        return new ConfigLong(getLong());
    }

    @Override
    public boolean equalsValue(ConfigValue value) {
        return getLong() == value.getLong();
    }

    @Override
    public Color4I getColor() {
        return ConfigInt.COLOR;
    }

    @Override
    public void addInfo(ConfigValueInstance inst, List<String> list) {
        super.addInfo(inst, list);

        long m = getMin();

        if (m != Long.MIN_VALUE) {
            list.add(EnumChatFormatting.AQUA + "Min: " + EnumChatFormatting.RESET + StringUtils.formatDouble(m));
        }

        m = getMax();

        if (m != Long.MAX_VALUE) {
            list.add(EnumChatFormatting.AQUA + "Max: " + EnumChatFormatting.RESET + StringUtils.formatDouble(m));
        }
    }

    @Override
    public boolean setValueFromString(@Nullable ICommandSender sender, String string, boolean simulate) {
        if (string.isEmpty()) {
            return false;
        }

        try {
            long l = Long.parseLong(string);

            if (!simulate) {
                setLong(l);
            }

            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String key) {
        value = getLong();

        if (value != 0L) {
            nbt.setLong(key, value);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String key) {
        setLong(nbt.getLong(key));
    }

    @Override
    public void writeData(DataOut data) {
        data.writeVarLong(getLong());
        data.writeVarLong(getMin());
        data.writeVarLong(getMax());
    }

    @Override
    public void readData(DataIn data) {
        setLong(data.readVarLong());
        setMin(data.readVarLong());
        setMax(data.readVarLong());
    }

    @Override
    public long getAsLong() {
        return getLong();
    }

    @Override
    public void setValueFromOtherValue(ConfigValue value) {
        setLong(value.getLong());
    }

    @Override
    public void setValueFromJson(JsonElement json) {
        if (json.isJsonPrimitive()) {
            setLong(json.getAsLong());
        }
    }
}
