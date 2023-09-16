package ftb.lib.api.config;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import latmod.lib.IntList;

public class ConfigEntryIntList extends ConfigEntry {

    public final IntList defValue;
    private IntList value;

    public ConfigEntryIntList(String id, IntList def) {
        super(id);
        defValue = def == null ? new IntList() : def.copy();
        value = defValue.copy();
    }

    public ConfigEntryType getConfigType() {
        return ConfigEntryType.INT_ARRAY;
    }

    public int getColor() {
        return 0xAA5AE8;
    }

    public ConfigEntryIntList(String id, int[] def) {
        this(id, IntList.asList(def));
    }

    public void set(IntList l) {
        value = (l == null || l.isEmpty()) ? new IntList() : l.copy();
    }

    public void func_152753_a(JsonElement o) {
        JsonArray a = o.getAsJsonArray();
        IntList l = new IntList(a.size());
        for (int i = 0; i < l.size(); i++) l.set(i, a.get(i).getAsInt());
        set(l);
    }

    public JsonElement getSerializableElement() {
        JsonArray a = new JsonArray();
        value = getAsIntList();
        for (int i = 0; i < value.size(); i++) a.add(new JsonPrimitive(value.get(i)));
        return a;
    }

    public String getAsString() {
        return getAsIntList().toString();
    }

    public List<String> getAsStringList() {
        List<String> l = new ArrayList<>();
        for (Integer i : getAsIntList()) l.add(i.toString());
        return l;
    }

    public IntList getAsIntList() {
        return value;
    }

    public String getDefValueString() {
        return defValue.toString();
    }

    public void writeToNBT(NBTTagCompound tag, boolean extended) {
        super.writeToNBT(tag, extended);

        int[] ai = getAsIntList().toArray();

        if (ai.length > 0) {
            tag.setIntArray("V", ai);
        }

        if (extended && !defValue.isEmpty()) {
            tag.setIntArray("D", defValue.toArray());
        }
    }

    public void readFromNBT(NBTTagCompound tag, boolean extended) {
        super.readFromNBT(tag, extended);
        set(tag.hasKey("V") ? new IntList(tag.getIntArray("V")) : null);

        if (extended) {
            defValue.clear();

            if (tag.hasKey("D")) {
                defValue.addAll(tag.getIntArray("D"));
            }
        }
    }
}
