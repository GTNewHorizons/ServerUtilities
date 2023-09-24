package serverutils.serverlib.lib.io;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.EncoderException;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.common.util.Constants;

import serverutils.serverlib.lib.icon.Icon;
import serverutils.serverlib.lib.math.BlockDimPos;
import serverutils.serverlib.lib.util.BlockUtils;
import serverutils.serverlib.lib.util.JsonUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class DataOut
{
	@FunctionalInterface
	public interface Serializer<T>
	{
		void write(DataOut data, T object);
	}

	public static final Serializer<String> STRING = DataOut::writeString;
	public static final Serializer<Integer> INT = DataOut::writeInt;
	public static final Serializer<Boolean> BOOLEAN = DataOut::writeBoolean;
	public static final Serializer<UUID> UUID = DataOut::writeUUID;
	public static final Serializer<BlockPos> BLOCK_POS = DataOut::writePos;
	public static final Serializer<BlockDimPos> BLOCK_DIM_POS = DataOut::writeDimPos;
	public static final Serializer<JsonElement> JSON = DataOut::writeJson;
	public static final Serializer<IChatComponent> TEXT_COMPONENT = DataOut::writeTextComponent;
	public static final Serializer<ResourceLocation> RESOURCE_LOCATION = DataOut::writeResourceLocation;
	public static final Serializer<ItemStack> ITEM_STACK = DataOut::writeItemStack;

	public static final DataOut.Serializer<ChunkPos> CHUNK_POS = (data, pos) ->
	{
		data.writeVarInt(pos.x);
		data.writeVarInt(pos.z);
	};

	private final ByteBuf byteBuf;

	public DataOut(ByteBuf io)
	{
		byteBuf = io;
	}

	public int getPosition()
	{
		return byteBuf.writerIndex();
	}

	public void writeBoolean(boolean value)
	{
		byteBuf.writeBoolean(value);
	}

	public void writeByte(int value)
	{
		byteBuf.writeByte(value);
	}

	public void writeBytes(byte[] bytes, int off, int len)
	{
		byteBuf.writeBytes(bytes, off, len);
	}

	public void writeBytes(byte[] bytes)
	{
		writeBytes(bytes, 0, bytes.length);
	}

	public void writeShort(int value)
	{
		byteBuf.writeShort(value);
	}

	public void writeInt(int value)
	{
		byteBuf.writeInt(value);
	}

	public void writeLong(long value)
	{
		byteBuf.writeLong(value);
	}

	public void writeFloat(float value)
	{
		byteBuf.writeFloat(value);
	}

	public void writeDouble(double value)
	{
		byteBuf.writeDouble(value);
	}

	public void writePos(Vec3i pos)
	{
		writeVarInt(pos.getX());
		writeVarInt(pos.getY());
		writeVarInt(pos.getZ());
	}

	public void writeDimPos(BlockDimPos pos)
	{
		writeVarInt(pos.dim);
		writeVarInt(pos.posX);
		writeVarInt(pos.posY);
		writeVarInt(pos.posZ);
	}

	public void writeUUID(UUID id)
	{
		writeLong(id.getMostSignificantBits());
		writeLong(id.getLeastSignificantBits());
	}

	public void writeString(String string)
	{
		if (string.isEmpty())
		{
			writeVarInt(0);
			return;
		}

		byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
		writeVarInt(bytes.length);
		writeBytes(bytes);
	}

	public <T> void writeCollection(Collection<T> collection, Serializer<T> serializer)
	{
		int size = collection.size();

		if (size == 0)
		{
			writeVarInt(0);
			return;
		}

		if (collection instanceof Set)
		{
			writeVarInt(-size);
		}
		else
		{
			writeVarInt(size);
		}

		for (T object : collection)
		{
			serializer.write(this, object);
		}
	}

	public <K, V> void writeMap(Map<K, V> map, Serializer<K> keySerializer, Serializer<V> valueSerializer)
	{
		int size = map.size();

		if (size == 0)
		{
			writeVarInt(0);
			return;
		}

		if (map instanceof LinkedHashMap || map instanceof SortedMap)
		{
			writeVarInt(-size);
		}
		else
		{
			writeVarInt(size);
		}

		for (Map.Entry<K, V> entry : map.entrySet())
		{
			keySerializer.write(this, entry.getKey());
			valueSerializer.write(this, entry.getValue());
		}
	}

	public void writeItemStack(ItemStack stack)
	{
		if (stack.isEmpty() || stack.getItem().getRegistryName() == null)
		{
			writeVarInt(0);
			return;
		}

		writeVarInt(Item.getIdFromItem(stack.getItem()));
		writeVarInt(stack.getCount());
		writeVarInt(stack.getMetadata());
		writeNBT(stack.getItem().isDamageable() || stack.getItem().getShareTag() ? stack.getItem().getNBTShareTag(stack) : null);
	}

	public void writeNBT(@Nullable NBTTagCompound nbt)
	{
		if (nbt == null)
		{
			writeByte(0);
		}
		else
		{
			try
			{
				CompressedStreamTools.write(nbt, new ByteBufOutputStream(byteBuf));
			}
			catch (IOException ex)
			{
				throw new EncoderException(ex);
			}
		}
	}

	public void writeNBTBase(@Nullable NBTBase nbt)
	{
		if (nbt == null || nbt.getId() == Constants.NBT.TAG_END)
		{
			writeByte(Constants.NBT.TAG_END);
			return;
		}

		writeByte(nbt.getId());

		switch (nbt.getId())
		{
			case Constants.NBT.TAG_BYTE:
				writeByte(((NBTBase.NBTPrimitive) nbt).func_150290_f());
				return;
			case Constants.NBT.TAG_SHORT:
				writeShort(((NBTBase.NBTPrimitive) nbt).func_150289_e());
				return;
			case Constants.NBT.TAG_INT:
				writeInt(((NBTBase.NBTPrimitive) nbt).func_150287_d());
				return;
			case Constants.NBT.TAG_LONG:
				writeLong(((NBTBase.NBTPrimitive) nbt).func_150291_c());
				return;
			case Constants.NBT.TAG_FLOAT:
				writeFloat(((NBTBase.NBTPrimitive) nbt).func_150288_h());
				return;
			case Constants.NBT.TAG_DOUBLE:
				writeDouble(((NBTBase.NBTPrimitive) nbt).func_150286_g());
				return;
			//TAG_BYTE_ARRAY
			case Constants.NBT.TAG_STRING:
				writeString(((NBTTagString) nbt).func_150285_a_());
				return;
			//TAG_LIST
			case Constants.NBT.TAG_COMPOUND:
				writeNBT((NBTTagCompound) nbt);
				return;
			//TAG_INT_ARRAY
			//TAG_LONG_ARRAY
			default:
				NBTTagCompound nbt1 = new NBTTagCompound();
				nbt1.setTag("_", nbt);
				writeNBT(nbt1);
		}
	}

	public void writeResourceLocation(ResourceLocation r)
	{
		writeString(r.toString());
	}

	public int writeJson(@Nullable JsonElement element)
	{
		if (JsonUtils.isNull(element))
		{
			writeByte(0);
			return 0;
		}
		else if (element.isJsonObject())
		{
			writeByte(1);

			Set<Map.Entry<String, JsonElement>> set = element.getAsJsonObject().entrySet();
			Map<String, JsonElement> map = new LinkedHashMap<>(set.size());

			for (Map.Entry<String, JsonElement> entry : set)
			{
				map.put(entry.getKey(), entry.getValue());
			}

			writeMap(map, STRING, JSON);
			return 1;
		}
		else if (element.isJsonArray())
		{
			writeByte(2);

			JsonArray json = element.getAsJsonArray();
			Collection<JsonElement> collection = new ArrayList<>(json.size());

			for (JsonElement json1 : json)
			{
				collection.add(json1);
			}

			writeCollection(collection, JSON);
			return 2;
		}

		JsonPrimitive primitive = element.getAsJsonPrimitive();

		if (primitive.isBoolean())
		{
			if (primitive.getAsBoolean())
			{
				writeByte(5);
				return 5;
			}
			else
			{
				writeByte(6);
				return 6;
			}
		}
		else if (primitive.isNumber())
		{
			if (primitive == JsonUtils.JSON_ZERO)
			{
				writeByte(4);
				return 4;
			}

			Number number = primitive.getAsNumber();

			if (number.doubleValue() == 0D)
			{
				writeByte(4);
				return 4;
			}

			Class<? extends Number> n = number.getClass();

			if (n == Float.class)
			{
				writeByte(8);
				writeFloat(primitive.getAsFloat());
				return 8;
			}
			else if (n == Double.class)
			{
				writeByte(9);
				writeDouble(primitive.getAsDouble());
				return 9;
			}
			else
			{
				writeByte(7);
				writeVarLong(primitive.getAsLong());
				return 7;
			}
		}

		String string = primitive.getAsString();

		if (string.isEmpty())
		{
			writeByte(10);
			return 10;
		}

		writeByte(3);
		writeString(string);
		return 3;
	}

	public void writeTextComponent(@Nullable IChatComponent component)
	{
		writeJson(JsonUtils.serializeTextComponent(component));
	}

	public void writeBlockState(IBlockState state)
	{
		writeInt(state == BlockUtils.AIR_STATE ? 0 : Block.getStateId(state));
	}

	public void writeIcon(@Nullable Icon icon)
	{
		writeJson(icon == null || icon.isEmpty() ? JsonNull.INSTANCE : icon.getJson());
	}

	public void writeIntList(IntCollection collection)
	{
		writeVarInt(collection.size());

		if (!collection.isEmpty())
		{
			IntIterator iterator = collection.iterator();

			while (iterator.hasNext())
			{
				writeInt(iterator.nextInt());
			}
		}
	}

	public void writeVarInt(int value)
	{
		if (value > Short.MAX_VALUE || value < Short.MIN_VALUE)
		{
			writeByte(123);
			writeInt(value);
		}
		else if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE)
		{
			writeByte(122);
			writeShort(value);
		}
		else if (value >= 121 && value <= 123)
		{
			writeByte(121);
			writeByte(value);
		}
		else
		{
			writeByte(value);
		}
	}

	public void writeVarLong(long value)
	{
		if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE)
		{
			writeByte(124);
			writeLong(value);
		}
		else if (value > Short.MAX_VALUE || value < Short.MIN_VALUE)
		{
			writeByte(123);
			writeInt((int) value);
		}
		else if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE)
		{
			writeByte(122);
			writeShort((int) value);
		}
		else if (value >= 121 && value <= 124)
		{
			writeByte(121);
			writeByte((int) value);
		}
		else
		{
			writeByte((int) value);
		}
	}
}