package ftb.lib.api.config;

import net.minecraft.nbt.NBTTagCompound;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import ftb.lib.api.IClickable;

public class ConfigEntryBool extends ConfigEntry implements IClickable {

    public boolean defValue;
    private boolean value;

    public ConfigEntryBool(String id, boolean def) {
        super(id);
        set(def);
        defValue = def;
    }

    public ConfigEntryType getConfigType() {
        return ConfigEntryType.BOOLEAN;
    }

    public int getColor() {
        return getAsBoolean() ? 0x33AA33 : 0xD52834;
    }

    public void set(boolean v) {
        value = v;
    }

    public void func_152753_a(JsonElement o) {
        set(o.getAsBoolean());
    }

    public JsonElement getSerializableElement() {
        return new JsonPrimitive(getAsBoolean());
    }

    public void onClicked(boolean leftClick) {
        set(!getAsBoolean());
    }

    public String getAsString() {
        return getAsBoolean() ? "true" : "false";
    }

    public boolean getAsBoolean() {
        return value;
    }

    public int getAsInt() {
        return getAsBoolean() ? 1 : 0;
    }

    public double getAsDouble() {
        return getAsBoolean() ? 1D : 0D;
    }

    public String getDefValueString() {
        return defValue ? "true" : "false";
    }

    public void writeToNBT(NBTTagCompound tag, boolean extended) {
        super.writeToNBT(tag, extended);
        tag.setBoolean("V", getAsBoolean());

        if (extended) {
            tag.setBoolean("D", defValue);
        }
    }

    public void readFromNBT(NBTTagCompound tag, boolean extended) {
        super.readFromNBT(tag, extended);
        set(tag.getBoolean("V"));

        if (extended) {
            defValue = tag.getBoolean("D");
        }
    }
}
