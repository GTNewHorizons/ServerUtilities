package serverutils.lib.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraftforge.common.util.Constants;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.EncoderException;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import serverutils.lib.icon.Icon;
import serverutils.lib.math.BlockDimPos;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.util.CommonUtils;
import serverutils.lib.util.InvUtils;
import serverutils.lib.util.JsonUtils;

public class DataIn {

    @FunctionalInterface
    public interface Deserializer<T> {

        T read(DataIn data);
    }

    public static final Deserializer<String> STRING = DataIn::readString;
    public static final Deserializer<Integer> INT = DataIn::readInt;
    public static final Deserializer<Boolean> BOOLEAN = DataIn::readBoolean;

    public static final Deserializer<UUID> UUID = DataIn::readUUID;
    public static final Deserializer<Vec3> BLOCK_POS = DataIn::readPos;
    public static final Deserializer<BlockDimPos> BLOCK_DIM_POS = DataIn::readDimPos;
    public static final Deserializer<JsonElement> JSON = DataIn::readJson;
    public static final Deserializer<IChatComponent> TEXT_COMPONENT = DataIn::readTextComponent;
    public static final Deserializer<ResourceLocation> RESOURCE_LOCATION = DataIn::readResourceLocation;
    public static final Deserializer<ItemStack> ITEM_STACK = DataIn::readItemStack;
    public static final Deserializer<ChunkDimPos> CHUNK_DIM_POS = DataIn::readChunkDimPos;

    public static final DataIn.Deserializer<ChunkCoordIntPair> CHUNK_POS = data -> {
        int x = data.readVarInt();
        int z = data.readVarInt();
        return new ChunkCoordIntPair(x, z);
    };

    private final ByteBuf byteBuf;

    public DataIn(ByteBuf io) {
        byteBuf = io;
    }

    public int getPosition() {
        return byteBuf.readerIndex();
    }

    public boolean isReadable() {
        return byteBuf.isReadable();
    }

    public boolean readBoolean() {
        return byteBuf.readBoolean();
    }

    public byte readByte() {
        return byteBuf.readByte();
    }

    public void readBytes(byte[] bytes, int off, int len) {
        byteBuf.readBytes(bytes, off, len);
    }

    public void readBytes(byte[] bytes) {
        readBytes(bytes, 0, bytes.length);
    }

    public short readUnsignedByte() {
        return byteBuf.readUnsignedByte();
    }

    public short readShort() {
        return byteBuf.readShort();
    }

    public int readUnsignedShort() {
        return byteBuf.readUnsignedShort();
    }

    public int readInt() {
        return byteBuf.readInt();
    }

    public long readLong() {
        return byteBuf.readLong();
    }

    public float readFloat() {
        return byteBuf.readFloat();
    }

    public double readDouble() {
        return byteBuf.readDouble();
    }

    public String readString() {
        int s = readVarInt();

        if (s == 0) {
            return "";
        }

        byte[] bytes = new byte[s];
        readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public <T> Collection<T> readCollection(@Nullable Collection<T> collection, Deserializer<T> deserializer) {
        if (collection != null) {
            collection.clear();
        }

        int num = readVarInt();

        if (num == 0) {
            return collection == null ? Collections.emptyList() : collection;
        }

        int size = Math.abs(num);

        if (collection == null) {
            boolean list = num > 0;

            if (size == 1) {
                return list ? Collections.singletonList(deserializer.read(this))
                        : Collections.singleton(deserializer.read(this));
            }

            collection = list ? new ArrayList<>(size) : new HashSet<>(size);
        }

        while (--size >= 0) {
            collection.add(deserializer.read(this));
        }

        return collection;
    }

    public <T> Collection<T> readCollection(Deserializer<T> deserializer) {
        return readCollection(null, deserializer);
    }

    public <K, V> Map<K, V> readMap(@Nullable Map<K, V> map, Deserializer<K> keyDeserializer,
            Deserializer<V> valueDeserializer) {
        if (map != null) {
            map.clear();
        }

        int num = readVarInt();

        if (num == 0) {
            return map == null ? Collections.emptyMap() : map;
        }

        int size = Math.abs(num);

        if (map == null) {
            boolean linked = num < 0;

            if (keyDeserializer == INT) {
                map = CommonUtils.cast(linked ? new HashMap<>(size) : new Int2ObjectOpenHashMap<V>(size));
            } else {
                map = linked ? new LinkedHashMap<>(size) : new HashMap<>(size);
            }
        }

        while (--size >= 0) {
            K key = keyDeserializer.read(this);
            V value = valueDeserializer.read(this);
            map.put(key, value);
        }

        return map;
    }

    public <K, V> Map<K, V> readMap(Deserializer<K> keyDeserializer, Deserializer<V> valueDeserializer) {
        return readMap(null, keyDeserializer, valueDeserializer);
    }

    public ItemStack readItemStack() {
        int id = readVarInt();

        if (id == 0) {
            return InvUtils.EMPTY_STACK;
        }

        Item item = Item.getItemById(id);

        if (item == null) {
            return InvUtils.EMPTY_STACK;
        }

        int size = readVarInt();
        int meta = readVarInt();
        ItemStack stack = new ItemStack(item, size, meta);
        stack.readFromNBT(readNBT());
        return stack;
    }

    @Nullable
    public NBTTagCompound readNBT() {
        int i = byteBuf.readerIndex();
        byte b0 = byteBuf.readByte();

        if (b0 == 0) {
            return null;
        }

        byteBuf.readerIndex(i);

        try {
            return CompressedStreamTools.func_152456_a(new ByteBufInputStream(byteBuf), NBTSizeTracker.field_152451_a); // read
        } catch (IOException ex) {
            throw new EncoderException(ex);
        }
    }

    @Nullable
    public NBTBase readNBTBase() {
        return switch (readByte()) {
            case Constants.NBT.TAG_END -> null;
            case Constants.NBT.TAG_BYTE -> new NBTTagByte(readByte());
            case Constants.NBT.TAG_SHORT -> new NBTTagShort(readShort());
            case Constants.NBT.TAG_INT -> new NBTTagInt(readInt());
            case Constants.NBT.TAG_LONG -> new NBTTagLong(readLong());
            case Constants.NBT.TAG_FLOAT -> new NBTTagFloat(readFloat());
            case Constants.NBT.TAG_DOUBLE -> new NBTTagDouble(readDouble());
            // TAG_BYTE_ARRAY
            case Constants.NBT.TAG_STRING -> new NBTTagString(readString());
            // TAG_LIST
            case Constants.NBT.TAG_COMPOUND -> readNBT();
            // TAG_INT_ARRAY
            // TAG_LONG_ARRAY
            default -> readNBT().getTag("_");
        };
    }

    public Vec3 readPos() {
        int x = readVarInt();
        int y = readVarInt();
        int z = readVarInt();
        return Vec3.createVectorHelper(x, y, z);
    }

    public BlockDimPos readDimPos() {
        int d = readVarInt();
        int x = readVarInt();
        int y = readVarInt();
        int z = readVarInt();
        return new BlockDimPos(x, y, z, d);
    }

    public ChunkDimPos readChunkDimPos() {
        int x = readVarInt();
        int z = readVarInt();
        int d = readVarInt();
        return new ChunkDimPos(x, z, d);
    }

    public UUID readUUID() {
        long msb = readLong();
        long lsb = readLong();
        return new UUID(msb, lsb);
    }

    public ResourceLocation readResourceLocation() {
        return new ResourceLocation(readString());
    }

    public JsonElement readJson() {
        switch (readUnsignedByte()) {
            case 0:
                return JsonNull.INSTANCE;
            case 1: {
                JsonObject json = new JsonObject();

                for (Map.Entry<String, JsonElement> entry : readMap(STRING, JSON).entrySet()) {
                    json.add(entry.getKey(), entry.getValue());
                }

                return json;
            }
            case 2: {
                JsonArray json = new JsonArray();

                for (JsonElement json1 : readCollection(JSON)) {
                    json.add(json1);
                }

                return json;
            }
            case 3: {
                String s = readString();
                return s.isEmpty() ? JsonUtils.JSON_EMPTY_STRING : new JsonPrimitive(s);
            }
            case 4:
                return JsonUtils.JSON_ZERO;
            case 5:
                return JsonUtils.JSON_TRUE;
            case 6:
                return JsonUtils.JSON_FALSE;
            case 7:
                return new JsonPrimitive(readVarLong());
            case 8:
                return new JsonPrimitive(readFloat());
            case 9:
                return new JsonPrimitive(readDouble());
            case 10:
                return JsonUtils.JSON_EMPTY_STRING;
        }

        return JsonNull.INSTANCE;
    }

    @Nullable
    public IChatComponent readTextComponent() {
        return JsonUtils.deserializeTextComponent(readJson());
    }

    public Icon readIcon() {
        return Icon.getIcon(readJson());
    }

    public IntArrayList readIntList() {
        int size = readVarInt();

        if (size == 0) {
            return new IntArrayList();
        }

        IntArrayList list = new IntArrayList();

        for (int i = 0; i < size; i++) {
            list.add(readInt());
        }

        return list;
    }

    public int readVarInt() {
        int b = readByte();

        return switch (b) {
            case 121 -> readByte();
            case 122 -> readShort();
            case 123 -> readInt();
            default -> b;
        };
    }

    public long readVarLong() {
        int b = readByte();

        return switch (b) {
            case 121 -> readByte();
            case 122 -> readShort();
            case 123 -> readInt();
            case 124 -> readLong();
            default -> b;
        };
    }
}
