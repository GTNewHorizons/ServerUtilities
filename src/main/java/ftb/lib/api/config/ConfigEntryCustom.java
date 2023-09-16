package ftb.lib.api.config;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import ftb.lib.api.IClickable;
import latmod.lib.IntList;

public class ConfigEntryCustom extends ConfigEntry implements IClickable {

    private JsonElement value;

    public ConfigEntryCustom(String id) {
        super(id);
    }

    public ConfigEntryType getConfigType() {
        return ConfigEntryType.CUSTOM;
    }

    public int getColor() {
        return 0xFFAA00;
    }

    public void func_152753_a(JsonElement o) {
        value = o == JsonNull.INSTANCE ? null : o;
    }

    public JsonElement getSerializableElement() {
        return value == null ? JsonNull.INSTANCE : value;
    }

    public ConfigGroup getAsGroup() {
        JsonElement e = getSerializableElement();
        if (e.isJsonNull()) return null;
        else if (e.isJsonObject()) {
            ConfigGroup group = new ConfigGroup(getID());
            group.func_152753_a(e);
            return group;
        }
        return null;
    }

    public String getAsString() {
        JsonElement e = getSerializableElement();
        return e.isJsonNull() ? ". . ." : String.valueOf(e);
    }

    public List<String> getAsStringList() {
        JsonElement e = getSerializableElement();
        if (e.isJsonNull()) return new ArrayList<>();
        List<String> list = new ArrayList<>();
        for (JsonElement e1 : e.getAsJsonArray()) list.add(e1.getAsString());
        return list;
    }

    public boolean getAsBoolean() {
        JsonElement e = getSerializableElement();
        return e.isJsonNull() ? false : e.getAsBoolean();
    }

    public int getAsInt() {
        JsonElement e = getSerializableElement();
        return e.isJsonNull() ? 0 : e.getAsInt();
    }

    public double getAsDouble() {
        JsonElement e = getSerializableElement();
        return e.isJsonNull() ? 0D : e.getAsDouble();
    }

    public IntList getAsIntList() {
        JsonElement e = getSerializableElement();
        if (e.isJsonNull()) return null;
        JsonArray a = e.getAsJsonArray();
        IntList l = new IntList(a.size());
        for (int i = 0; i < l.size(); i++) l.set(i, a.get(i).getAsInt());
        return l;
    }

    public void onClicked(boolean leftClick) {}

    public void writeToNBT(NBTTagCompound tag, boolean extended) {
        super.writeToNBT(tag, extended);
    }

    public void readFromNBT(NBTTagCompound tag, boolean extended) {
        super.readFromNBT(tag, extended);
    }
}
