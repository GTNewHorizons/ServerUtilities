package ftb.lib.api.config;

import net.minecraft.nbt.NBTTagCompound;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import latmod.lib.LMStringUtils;
import latmod.lib.MathHelperLM;
import latmod.lib.annotations.INumberBoundsContainer;

public class ConfigEntryDouble extends ConfigEntry implements INumberBoundsContainer {

    public double defValue;
    private double value;
    private Double minValue, maxValue;

    public ConfigEntryDouble(String id, double d) {
        super(id);
        defValue = d;
        set(d);
    }

    public ConfigEntryType getConfigType() {
        return ConfigEntryType.DOUBLE;
    }

    public int getColor() {
        return 0xAA5AE8;
    }

    public void setBounds(double min, double max) {
        minValue = min == Double.NEGATIVE_INFINITY ? null : min;
        maxValue = max == Double.POSITIVE_INFINITY ? null : max;
    }

    public double getMin() {
        return minValue == null ? Double.NEGATIVE_INFINITY : minValue;
    }

    public double getMax() {
        return maxValue == null ? Double.POSITIVE_INFINITY : maxValue;
    }

    public void set(double v) {
        value = MathHelperLM.clamp(v, getMin(), getMax());
    }

    public void add(double v) {
        set(getAsDouble() + v);
    }

    public final void func_152753_a(JsonElement o) {
        set(o.getAsDouble());
    }

    public final JsonElement getSerializableElement() {
        return new JsonPrimitive(getAsDouble());
    }

    public String getAsString() {
        return Double.toString(getAsDouble());
    }

    public int getAsInt() {
        return (int) getAsDouble();
    }

    public double getAsDouble() {
        return value;
    }

    public String getDefValueString() {
        return Double.toString(defValue);
    }

    public String getMinValueString() {
        double d = getMin();

        if (d != Double.NEGATIVE_INFINITY) {
            return LMStringUtils.formatDouble(d);
        }

        return null;
    }

    public String getMaxValueString() {
        double d = getMax();

        if (d != Double.POSITIVE_INFINITY) {
            return LMStringUtils.formatDouble(d);
        }

        return null;
    }

    public void writeToNBT(NBTTagCompound tag, boolean extended) {
        super.writeToNBT(tag, extended);

        double d = getAsDouble();

        if (d != 0D) tag.setDouble("V", d);

        if (extended) {
            if (defValue != 0D) tag.setDouble("D", defValue);

            d = getMin();

            if (d != Double.NEGATIVE_INFINITY) tag.setDouble("MN", d);

            d = getMax();

            if (d != Double.POSITIVE_INFINITY) tag.setDouble("MX", d);
        }
    }

    public void readFromNBT(NBTTagCompound tag, boolean extended) {
        super.readFromNBT(tag, extended);
        set(tag.getDouble("V"));

        if (extended) {
            defValue = tag.getDouble("D");
            setBounds(
                    tag.hasKey("MN") ? tag.getDouble("MN") : Double.NEGATIVE_INFINITY,
                    tag.hasKey("MX") ? tag.getDouble("MX") : Double.POSITIVE_INFINITY);
        }
    }
}
