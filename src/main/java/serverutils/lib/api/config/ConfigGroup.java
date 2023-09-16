package serverutils.lib.api.config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import latmod.lib.LMStringUtils;
import latmod.lib.LMUtils;
import latmod.lib.annotations.AnnotationHelper;
import latmod.lib.annotations.Flags;
import serverutils.lib.LMNBTUtils;

public class ConfigGroup extends ConfigEntry {

    public final LinkedHashMap<String, ConfigEntry> entryMap;
    private String displayName;

    public ConfigGroup(String s) {
        super(s);
        entryMap = new LinkedHashMap<>();
    }

    public ConfigEntryType getConfigType() {
        return ConfigEntryType.GROUP;
    }

    public final List<ConfigEntry> sortedEntries() {
        List<ConfigEntry> list = new ArrayList<>();
        list.addAll(entryMap.values());
        Collections.sort(list, null);
        return list;
    }

    public ConfigFile getConfigFile() {
        return (parentGroup != null) ? parentGroup.getConfigFile() : null;
    }

    public ConfigGroup add(ConfigEntry e, boolean copy) {
        if (e != null) {
            if (copy) {
                ConfigEntry e1 = e.copy();
                entryMap.put(e1.getID(), e1);
                e1.parentGroup = this;
            } else {
                entryMap.put(e.getID(), e);
                e.parentGroup = this;
            }
        }

        return this;
    }

    public ConfigGroup addAll(Class<?> c, Object parent, boolean copy) {
        try {
            Field f[] = c.getDeclaredFields();

            if (f != null && f.length > 0) for (Field aF : f) {
                try {
                    aF.setAccessible(true);
                    if (ConfigEntry.class.isAssignableFrom(aF.getType())) {
                        ConfigEntry entry = (ConfigEntry) aF.get(parent);
                        if (entry != null && entry != this && !(entry instanceof ConfigFile)) {
                            AnnotationHelper.inject(aF, parent, entry);
                            add(entry, copy);
                        }
                    }
                } catch (Exception e1) {}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return this;
    }

    public ConfigEntry copy() {
        ConfigGroup g = new ConfigGroup(getID());
        for (ConfigEntry e : entryMap.values()) g.add(e, true);
        return g;
    }

    public final void func_152753_a(JsonElement o0) {
        if (o0 == null || !o0.isJsonObject()) return;

        entryMap.clear();

        JsonObject o = o0.getAsJsonObject();

        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            ConfigEntry entry = new ConfigEntryCustom(e.getKey());

            if (!e.getValue().isJsonNull()) {
                entry.func_152753_a(e.getValue());
            }

            add(entry, false);
        }
    }

    public final JsonElement getSerializableElement() {
        JsonObject o = new JsonObject();

        for (ConfigEntry e : entryMap.values()) {
            if (!e.getFlag(Flags.EXCLUDED)) {
                o.add(e.getID(), e.getSerializableElement());
            }
        }

        return o;
    }

    public String getAsString() {
        return getSerializableElement().toString();
    }

    public List<String> getAsStringList() {
        List<String> list = new ArrayList<>(entryMap.size());
        for (ConfigEntry e : entryMap.values()) {
            list.add(e.getAsString());
        }

        return list;
    }

    public boolean getAsBoolean() {
        return !entryMap.isEmpty();
    }

    public int getAsInt() {
        return entryMap.size();
    }

    public final void setDisplayName(String s) {
        displayName = (s == null || s.isEmpty()) ? null : s;
    }

    public final String getDisplayName() {
        return displayName == null ? LMStringUtils.firstUppercase(getID()) : displayName;
    }

    public void writeToNBT(NBTTagCompound tag, boolean extended) {
        super.writeToNBT(tag, extended);

        if (extended && displayName != null) {
            tag.setString("N", displayName);
        }

        if (!entryMap.isEmpty()) {
            NBTTagCompound tag1 = new NBTTagCompound();

            for (ConfigEntry e : entryMap.values()) {
                NBTTagCompound tag2 = new NBTTagCompound();
                e.writeToNBT(tag2, extended);
                tag2.setByte("T", e.getConfigType().ID);
                tag1.setTag(e.getID(), tag2);
            }

            tag.setTag("V", tag1);
        }
    }

    public void readFromNBT(NBTTagCompound tag, boolean extended) {
        super.readFromNBT(tag, extended);

        if (extended) {
            displayName = tag.hasKey("N") ? tag.getString("N") : null;
        }

        entryMap.clear();

        if (tag.hasKey("V")) {
            for (Map.Entry<String, NBTBase> entry : LMNBTUtils.entrySet((NBTTagCompound) tag.getTag("V"))) {
                NBTTagCompound tag2 = (NBTTagCompound) entry.getValue();
                ConfigEntryType t = ConfigEntryType.getFromID(tag2.getByte("T"));

                if (t != null) {
                    ConfigEntry e = t.createNew(entry.getKey());
                    e.readFromNBT(tag2, extended);
                    entryMap.put(e.getID(), e);
                }
            }
        }
    }

    public int loadFromGroup(ConfigGroup l, boolean isNBT) {
        if (l == null || l.entryMap.isEmpty()) return 0;

        int result = 0;

        for (ConfigEntry e1 : l.entryMap.values()) {
            ConfigEntry e0 = entryMap.get(e1.getID());

            if (e0 != null) {
                if (e0.getAsGroup() != null) {
                    ConfigGroup g1 = new ConfigGroup(e1.getID());

                    if (isNBT) {
                        NBTTagCompound tag = new NBTTagCompound();
                        e1.writeToNBT(tag, false);
                        g1.readFromNBT(tag, false);
                    } else {
                        g1.func_152753_a(e1.getSerializableElement());
                    }

                    result += e0.getAsGroup().loadFromGroup(g1, isNBT);
                } else {
                    try {
                        if (isNBT) {
                            NBTTagCompound tag = new NBTTagCompound();
                            e1.writeToNBT(tag, false);
                            e0.readFromNBT(tag, false);
                        } else {
                            e0.func_152753_a(e1.getSerializableElement());
                        }

                        result++;
                    } catch (Exception ex) {
                        System.err.println(
                                "Can't set value " + e1.getAsString()
                                        + " for '"
                                        + e0.parentGroup.getID()
                                        + "."
                                        + e0.getID()
                                        + "' (type:"
                                        + e0.getConfigType()
                                        + ")");
                        System.err.println(ex.toString());
                    }
                }
            }
        }

        if (result > 0) onLoadedFromGroup(l);
        return result;
    }

    protected void onLoadedFromGroup(ConfigGroup l) {}

    public boolean hasKey(Object key) {
        return entryMap.containsKey(LMUtils.getID(key));
    }

    public ConfigEntry getEntry(Object key) {
        return entryMap.get(LMUtils.getID(key));
    }

    public ConfigGroup getGroup(Object key) {
        ConfigEntry e = getEntry(key);
        return (e == null) ? null : e.getAsGroup();
    }

    public List<ConfigGroup> getGroups() {
        ArrayList<ConfigGroup> list = new ArrayList<>();
        for (ConfigEntry e : entryMap.values()) {
            ConfigGroup g = e.getAsGroup();
            if (g != null) list.add(g);
        }
        return list;
    }

    public ConfigGroup getAsGroup() {
        return this;
    }

    public int getTotalEntryCount() {
        int count = 0;

        for (ConfigEntry e : entryMap.values()) {
            if (e.getAsGroup() == null) count++;
            else count += e.getAsGroup().getTotalEntryCount();
        }

        return count;
    }

    public int getDepth() {
        return (parentGroup == null) ? 0 : (parentGroup.getDepth() + 1);
    }

    public ConfigGroup generateSynced(boolean copy) {
        ConfigGroup out = new ConfigGroup(getID());

        for (ConfigEntry e : entryMap.values()) {
            if (e.getFlag(Flags.SYNC)) {
                out.add(e, copy);
            } else if (e.getAsGroup() != null) {
                ConfigGroup g = e.getAsGroup().generateSynced(copy);
                if (!g.entryMap.isEmpty()) out.add(g, false);
            }
        }

        return out;
    }
}
