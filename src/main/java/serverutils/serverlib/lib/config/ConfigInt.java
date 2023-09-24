package serverutils.serverlib.lib.config;

import com.mojang.realmsclient.gui.ChatFormatting;
import serverutils.serverlib.lib.icon.Color4I;
import serverutils.serverlib.lib.io.DataIn;
import serverutils.serverlib.lib.io.DataOut;
import com.google.gson.JsonElement;
import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * @author LatvianModder
 */
public class ConfigInt extends ConfigValue implements IntSupplier
{
	public static final String ID = "int";
	public static final Color4I COLOR = Color4I.rgb(0xAA5AE8);

	public static class SimpleInt extends ConfigInt
	{
		private final IntSupplier get;
		private final IntConsumer set;

		public SimpleInt(int min, int max, IntSupplier g, IntConsumer s)
		{
			super(0, min, max);
			get = g;
			set = s;
		}

		@Override
		public int getInt()
		{
			return get.getAsInt();
		}

		@Override
		public void setInt(int v)
		{
			set.accept(v);
		}
	}

	private int value;
	private int min = Integer.MIN_VALUE;
	private int max = Integer.MAX_VALUE;

	public ConfigInt()
	{
	}

	public ConfigInt(int v)
	{
		value = v;
	}

	public ConfigInt(int v, int mn, int mx)
	{
		this(MathHelper.clamp(v, mn, mx));
		min = mn;
		max = mx;
	}

	@Override
	public String getId()
	{
		return ID;
	}

	public ConfigInt setMin(int v)
	{
		min = v;
		return this;
	}

	public ConfigInt setMax(int v)
	{
		max = v;
		return this;
	}

	public int getMin()
	{
		return min;
	}

	public int getMax()
	{
		return max;
	}

	public void setInt(int v)
	{
		value = MathHelper.clamp(v, getMin(), getMax());
	}

	@Override
	public String getString()
	{
		return Integer.toString(getInt());
	}

	@Override
	public boolean getBoolean()
	{
		return getInt() != 0;
	}

	@Override
	public int getInt()
	{
		return value;
	}

	@Override
	public ConfigInt copy()
	{
		return new ConfigInt(getInt(), getMin(), getMax());
	}

	@Override
	public boolean equalsValue(ConfigValue value)
	{
		return getInt() == value.getInt();
	}

	@Override
	public Color4I getColor()
	{
		return COLOR;
	}

	@Override
	public void addInfo(ConfigValueInstance inst, List<String> list)
	{
		super.addInfo(inst, list);

		int m = getMin();

		if (m != Integer.MIN_VALUE)
		{
			list.add(ChatFormatting.AQUA + "Min: " + ChatFormatting.RESET + m);
		}

		m = getMax();

		if (m != Integer.MAX_VALUE)
		{
			list.add(ChatFormatting.AQUA + "Max: " + ChatFormatting.RESET + m);
		}
	}

	@Override
	public boolean setValueFromString(@Nullable ICommandSender sender, String string, boolean simulate)
	{
		try
		{
			int val = string.startsWith("#") ? Long.decode(string).intValue() : Integer.parseInt(string);

			if (val < getMin() || val > getMax())
			{
				return false;
			}

			if (!simulate)
			{
				setInt(val);
			}

			return true;
		}
		catch (Exception ex)
		{
			return false;
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String key)
	{
		value = getInt();

		if (value != 0)
		{
			nbt.setDouble(key, value);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String key)
	{
		setInt(nbt.getInteger(key));
	}

	@Override
	public void writeData(DataOut data)
	{
		data.writeVarInt(getInt());
		data.writeVarInt(getMin());
		data.writeVarInt(getMax());
	}

	@Override
	public void readData(DataIn data)
	{
		setInt(data.readVarInt());
		setMin(data.readVarInt());
		setMax(data.readVarInt());
	}

	@Override
	public int getAsInt()
	{
		return getInt();
	}

	@Override
	public void setValueFromOtherValue(ConfigValue value)
	{
		setInt(value.getInt());
	}

	@Override
	public void setValueFromJson(JsonElement json)
	{
		if (json.isJsonPrimitive())
		{
			setInt(json.getAsInt());
		}
	}
}