package ftb.lib.api.config;

import net.minecraft.nbt.NBTTagCompound;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import latmod.lib.MathHelperLM;
import latmod.lib.annotations.INumberBoundsContainer;

public class ConfigEntryInt extends ConfigEntry implements INumberBoundsContainer {

    public int defValue;
    private int value;
    private Integer minValue, maxValue;

    public ConfigEntryInt(String id, int def) {
        super(id);
        defValue = def;
        set(def);
    }

    public ConfigEntryType getConfigType() {
        return ConfigEntryType.INT;
    }

    public int getColor() {
        return 0xAA5AE8;
    }

    public void setBounds(double min, double max) {
        minValue = min == Double.NEGATIVE_INFINITY ? null : (int) min;
        maxValue = max == Double.POSITIVE_INFINITY ? null : (int) max;
    }

    public double getMin() {
        return minValue == null ? Double.NEGATIVE_INFINITY : minValue.doubleValue();
    }

    public double getMax() {
        return maxValue == null ? Double.POSITIVE_INFINITY : maxValue.doubleValue();
    }

    public void set(int v) {
        value = MathHelperLM.clampInt(v, (int) getMin(), (int) getMax());
    }

    public void add(int i) {
        set(getAsInt() + i);
    }

    public final void func_152753_a(JsonElement o) {
        set((o == null || o.isJsonNull()) ? defValue : o.getAsInt());
    }

    public final JsonElement getSerializableElement() {
        return new JsonPrimitive(getAsInt());
    }

    public String getAsString() {
        return Integer.toString(getAsInt());
    }

    public boolean getAsBoolean() {
        return getAsInt() != 0;
    }

    public int getAsInt() {
        return value;
    }

    public double getAsDouble() {
        return getAsInt();
    }

    public String getDefValueString() {
        return Integer.toString(defValue);
    }

    public String getMinValueString() {
        double d = getMin();

        if (d != Double.NEGATIVE_INFINITY) {
            return Integer.toString((int) d);
        }

        return null;
    }

    public String getMaxValueString() {
        double d = getMax();

        if (d != Double.POSITIVE_INFINITY) {
            return Integer.toString((int) d);
        }

        return null;
    }

    public void writeToNBT(NBTTagCompound tag, boolean extended) {
        super.writeToNBT(tag, extended);
        tag.setInteger("V", getAsInt());

        if (extended) {
            tag.setInteger("D", defValue);

            double d = getMin();

            if (d != Double.NEGATIVE_INFINITY) {
                tag.setInteger("MN", (int) d);
            }

            d = getMax();

            if (d != Double.POSITIVE_INFINITY) {
                tag.setInteger("MX", (int) d);
            }
        }
    }

    public void readFromNBT(NBTTagCompound tag, boolean extended) {
        super.readFromNBT(tag, extended);
        set(tag.getInteger("V"));

        if (extended) {
            defValue = tag.getInteger("D");
            setBounds(
                    tag.hasKey("MN") ? tag.getInteger("MN") : Double.NEGATIVE_INFINITY,
                    tag.hasKey("MX") ? tag.getInteger("MX") : Double.POSITIVE_INFINITY);
        }
    }
}
