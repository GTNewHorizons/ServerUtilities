package serverutils.lib.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.util.Constants;

import com.google.gson.JsonElement;

import serverutils.lib.data.ServerUtilitiesAPI;
import serverutils.lib.gui.IOpenableGui;
import serverutils.lib.gui.misc.GuiEditConfigList;
import serverutils.lib.icon.Color4I;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.util.misc.MouseButton;

public class ConfigList<T extends ConfigValue> extends ConfigValue implements Iterable<T> {

    public static final String ID = "list";
    public static final Color4I COLOR = Color4I.rgb(0xFFAA49);

    public static class SimpleList<C extends ConfigValue, V> extends ConfigList<C> {

        public final Collection<V> collection;
        public final Function<V, C> toConfig;
        public final Function<C, V> fromConfig;

        public SimpleList(Collection<V> c, C type, Function<V, C> to, Function<C, V> from) {
            super(type);
            collection = c;
            toConfig = to;
            fromConfig = from;
        }

        @Override
        public void readFromList() {
            if (list.size() == collection.size() && collection instanceof List) {
                for (int i = 0; i < list.size(); i++) {
                    ((List<V>) collection).set(i, fromConfig.apply(list.get(i)));
                }

                return;
            }

            collection.clear();

            for (C value : list) {
                collection.add(fromConfig.apply(value));
            }
        }

        @Override
        public void writeToList() {
            list.clear();

            for (V value : collection) {
                list.add(toConfig.apply(value));
            }
        }
    }

    public final List<T> list;
    public T type;

    public ConfigList(T t) {
        list = new ArrayList<>();
        type = t;
    }

    @Override
    public String getId() {
        return ID;
    }

    public boolean canAdd(ConfigValue value) {
        return !value.isNull() && type.getId().equals(value.getId());
    }

    public ConfigList<T> add(ConfigValue v) {
        if (canAdd(v)) {
            writeToList();
            ConfigValue v1 = type.copy();
            v1.setValueFromOtherValue(v);
            list.add((T) v1);
            readFromList();
        }

        return this;
    }

    @Override
    public void writeData(DataOut data) {
        writeToList();
        data.writeString(type.getId());
        type.writeData(data);
        data.writeVarInt(list.size());

        for (ConfigValue s : list) {
            s.writeData(data);
        }
    }

    @Override
    public void readData(DataIn data) {
        list.clear();
        type = (T) ServerUtilitiesAPI.createConfigValueFromId(data.readString());
        type.readData(data);
        int s = data.readVarInt();

        while (--s >= 0) {
            ConfigValue v = type.copy();
            v.readData(data);
            list.add((T) v);
        }

        readFromList();
    }

    @Override
    public String getString() {
        writeToList();
        StringBuilder builder = new StringBuilder("[");

        for (int i = 0; i < list.size(); i++) {
            builder.append(list.get(i).getString());

            if (i != list.size() - 1) {
                builder.append(',');
                builder.append(' ');
            }
        }

        builder.append(']');
        return builder.toString();
    }

    @Override
    public boolean getBoolean() {
        return !list.isEmpty();
    }

    @Override
    public int getInt() {
        return list.size();
    }

    @Override
    public ConfigList<T> copy() {
        writeToList();
        ConfigList<T> l = new ConfigList<>((T) type.copy());

        for (T value : list) {
            l.list.add((T) value.copy());
        }

        return l;
    }

    @Override
    public Color4I getColor() {
        return COLOR;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String key) {
        writeToList();

        NBTTagList l = new NBTTagList();

        for (T value : list) {
            NBTTagCompound nbt1 = new NBTTagCompound();
            value.writeToNBT(nbt1, "value");
            l.appendTag(nbt1);
        }

        nbt.setTag(key, l);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String key) {
        list.clear();

        NBTTagList l = nbt.getTagList(key, Constants.NBT.TAG_COMPOUND);

        for (int i = 0; i < l.tagCount(); i++) {
            ConfigValue v = type.copy();
            v.readFromNBT(l.getCompoundTagAt(i), "value");
            list.add((T) v);
        }

        readFromList();
    }

    @Override
    public void addInfo(ConfigValueInstance inst, List<String> l) {
        l.add(EnumChatFormatting.AQUA + "Type: " + EnumChatFormatting.RESET + type.getId());

        if (list.isEmpty()) {
            l.add(EnumChatFormatting.AQUA + "Value: []");
        } else {
            l.add(EnumChatFormatting.AQUA + "Value: [");

            for (T value : list) {
                l.add("  " + value.getStringForGUI().getFormattedText());
            }

            l.add(EnumChatFormatting.AQUA + "]");
        }

        if (inst.getCanEdit() && inst.getDefaultValue() instanceof ConfigList) {
            ConfigList<T> val = (ConfigList<T>) inst.getDefaultValue();

            if (val.list.isEmpty()) {
                l.add(EnumChatFormatting.AQUA + "Default: []");
            } else {
                l.add(EnumChatFormatting.AQUA + "Default: [");

                for (T value : val.list) {
                    l.add("  " + value.getStringForGUI().getFormattedText());
                }

                l.add(EnumChatFormatting.AQUA + "]");
            }
        }
    }

    @Override
    public void onClicked(IOpenableGui gui, ConfigValueInstance inst, MouseButton button, Runnable callback) {
        new GuiEditConfigList(inst, callback).openGui();
    }

    @Override
    public IChatComponent getStringForGUI() {
        return new ChatComponentText("...");
    }

    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public void setValueFromOtherValue(ConfigValue ovalue) {
        list.clear();

        if (ovalue instanceof ConfigList && !ovalue.isEmpty() && type.equals(((ConfigList) ovalue).type)) {
            for (ConfigValue v : (ConfigList<?>) ovalue) {
                ConfigValue value = type.copy();
                value.setValueFromOtherValue(v);
                list.add((T) value);
            }
        }

        readFromList();
    }

    @Override
    public void setValueFromJson(JsonElement json) {
        list.clear();

        if (json.isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray()) {
                ConfigValue value = type.copy();
                value.setValueFromJson(e);
                list.add((T) value);
            }
        }

        readFromList();
    }

    public void readFromList() {}

    public void writeToList() {}
}
