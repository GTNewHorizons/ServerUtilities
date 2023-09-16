package ftb.lib.api.config;

import net.minecraft.nbt.NBTTagCompound;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class ConfigEntryString extends ConfigEntry {

    public String defValue;
    private String value;

    public ConfigEntryString(String id, String def) {
        super(id);
        set(def);
        defValue = def == null ? "" : def;
    }

    public ConfigEntryType getConfigType() {
        return ConfigEntryType.STRING;
    }

    public int getColor() {
        return 0xFFAA49;
    }

    public void set(String o) {
        value = o == null ? "" : o;
    }

    public final void func_152753_a(JsonElement o) {
        set(o.getAsString());
    }

    public final JsonElement getSerializableElement() {
        return new JsonPrimitive(getAsString());
    }

    public String getAsString() {
        return value;
    }

    public boolean getAsBoolean() {
        return getAsString().equals("true");
    }

    public int getAsInt() {
        return Integer.parseInt(getAsString());
    }

    public double getAsDouble() {
        return Double.parseDouble(getAsString());
    }

    public String getDefValueString() {
        return defValue;
    }

    public void writeToNBT(NBTTagCompound tag, boolean extended) {
        super.writeToNBT(tag, extended);

        String s = getAsString();
        if (!s.isEmpty()) tag.setString("V", s);

        if (extended && !defValue.isEmpty()) {
            tag.setString("D", defValue);
        }
    }

    public void readFromNBT(NBTTagCompound tag, boolean extended) {
        super.readFromNBT(tag, extended);
        set(tag.getString("V"));

        if (extended) {
            defValue = tag.getString("D");
        }
    }
}
