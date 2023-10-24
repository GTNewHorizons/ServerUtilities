package serverutils.lib.util.misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.util.Constants;

import serverutils.lib.icon.Color4I;
import serverutils.lib.icon.Icon;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.math.MathUtils;
import serverutils.lib.tile.EnumSaveType;
import serverutils.lib.util.CommonUtils;
import serverutils.lib.util.StringUtils;

public final class NameMap<E> implements Iterable<E>, DataIn.Deserializer<E>, DataOut.Serializer<E> {

    public static class ObjectProperties<T> {

        public static final ObjectProperties<Object> DEFAULT = new ObjectProperties<>();

        public String getName(T object) {
            return StringUtils.getID(object, StringUtils.FLAG_ID_ONLY_LOWERCASE | StringUtils.FLAG_ID_FIX);
        }

        public IChatComponent getDisplayName(@Nullable ICommandSender sender, T value) {
            return new ChatComponentText(getName(value));
        }

        public Color4I getColor(T object) {
            return Icon.EMPTY;
        }
    }

    @SafeVarargs
    public static <T> NameMap<T> create(T defaultValue, ObjectProperties<T> objectProperties, T... values) {
        List<T> list = new ArrayList<>(values.length);

        for (T e : values) {
            if (e != null) {
                list.add(e);
            }
        }

        if (list.isEmpty()) {
            throw new IllegalStateException("Value list can't be empty!");
        }

        return new NameMap<>(defaultValue, objectProperties, list);
    }

    @SafeVarargs
    public static <T> NameMap<T> create(T defaultValue, T... values) {
        return create(defaultValue, CommonUtils.cast(ObjectProperties.DEFAULT), values);
    }

    @SafeVarargs
    public static <T> NameMap<T> createWithName(T defaultValue,
            BiFunction<ICommandSender, T, IChatComponent> nameGetter, T... values) {
        return create(defaultValue, new ObjectProperties<T>() {

            @Override
            public IChatComponent getDisplayName(@Nullable ICommandSender sender, T value) {
                return nameGetter.apply(sender, value);
            }
        }, values);
    }

    @SafeVarargs
    public static <T> NameMap<T> createWithNameAndColor(T defaultValue,
            BiFunction<ICommandSender, T, IChatComponent> nameGetter, Function<T, Color4I> colorGetter, T... values) {
        return create(defaultValue, new ObjectProperties<T>() {

            @Override
            public IChatComponent getDisplayName(@Nullable ICommandSender sender, T value) {
                return nameGetter.apply(sender, value);
            }

            @Override
            public Color4I getColor(T object) {
                return colorGetter.apply(object);
            }
        }, values);
    }

    @SafeVarargs
    public static <T> NameMap<T> createWithTranslation(T defaultValue, BiFunction<ICommandSender, T, String> nameGetter,
            T... values) {
        return create(defaultValue, new ObjectProperties<T>() {

            @Override
            public IChatComponent getDisplayName(@Nullable ICommandSender sender, T value) {
                return new ChatComponentTranslation(nameGetter.apply(sender, value));
            }
        }, values);
    }

    @SafeVarargs
    public static <T> NameMap<T> createWithBaseTranslationKey(T defaultValue, String baseTranslationKey, T... values) {
        return create(defaultValue, new ObjectProperties<T>() {

            @Override
            public IChatComponent getDisplayName(@Nullable ICommandSender sender, T value) {
                return new ChatComponentTranslation(baseTranslationKey + "." + getName(value));
            }
        }, values);
    }

    private final ObjectProperties<E> objectProperties;
    public final E defaultValue;
    public final Map<String, E> map;
    public final List<String> keys;
    public final List<E> values;

    private NameMap(E def, ObjectProperties<E> ng, List<E> v) {
        objectProperties = ng;
        values = v;

        Map<String, E> map0 = new LinkedHashMap<>(size());

        for (E value : values) {
            map0.put(getName(value), value);
        }

        map = Collections.unmodifiableMap(map0);
        keys = Collections.unmodifiableList(new ArrayList<>(map.keySet()));
        defaultValue = get(getName(def));
    }

    private NameMap(E def, NameMap<E> n) {
        objectProperties = n.objectProperties;
        map = n.map;
        keys = n.keys;
        values = n.values;
        defaultValue = get(getName(def));
    }

    public String getName(E value) {
        return objectProperties.getName(value);
    }

    public IChatComponent getDisplayName(@Nullable ICommandSender sender, E value) {
        return objectProperties.getDisplayName(sender, value);
    }

    public Color4I getColor(E value) {
        return objectProperties.getColor(value);
    }

    public NameMap<E> withDefault(E def) {
        if (def == defaultValue) {
            return this;
        }

        return new NameMap<>(def, this);
    }

    public int size() {
        return values.size();
    }

    public E get(@Nullable String s) {
        if (s == null || s.isEmpty() || s.charAt(0) == '-') {
            return defaultValue;
        } else {
            E e = map.get(s);
            return e == null ? defaultValue : e;
        }
    }

    @Nullable
    public E getNullable(@Nullable String s) {
        if (s == null || s.isEmpty() || s.charAt(0) == '-') {
            return null;
        } else {
            return map.get(s);
        }
    }

    public E get(int index) {
        return index < 0 || index >= size() ? defaultValue : values.get(index);
    }

    public E offset(E value, int index) {
        return get(MathUtils.mod(getIndex(value) + index, size()));
    }

    public E getNext(E value) {
        return offset(value, 1);
    }

    public E getPrevious(E value) {
        return offset(value, -1);
    }

    public int getIndex(E e) {
        return values.indexOf(e);
    }

    public int getStringIndex(String s) {
        return getIndex(map.get(s));
    }

    public void writeToNBT(NBTTagCompound nbt, String name, EnumSaveType type, E value) {
        if (value == defaultValue) {
            return;
        }

        if (!type.save) {
            int index = getIndex(value);

            if (index == 0) {
                return;
            }

            if (size() >= 128) {
                nbt.setShort(name, (short) index);
            } else {
                nbt.setByte(name, (byte) index);
            }
        } else {
            nbt.setString(name, getName(value));
        }
    }

    public E readFromNBT(NBTTagCompound nbt, String name, EnumSaveType type) {
        if (!nbt.hasKey(name)) {
            return defaultValue;
        }

        return (!type.save || nbt.hasKey(name, Constants.NBT.TAG_ANY_NUMERIC)) ? get(nbt.getInteger(name))
                : get(nbt.getString(name));
    }

    public E getRandom(Random rand) {
        return values.get(rand.nextInt(size()));
    }

    @Override
    public Iterator<E> iterator() {
        return values.iterator();
    }

    @Override
    public void write(DataOut data, E object) {
        data.writeVarInt(getIndex(object));
    }

    @Override
    public E read(DataIn data) {
        return get(data.readVarInt());
    }
}
