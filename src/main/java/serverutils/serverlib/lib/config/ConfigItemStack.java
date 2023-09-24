package serverutils.serverlib.lib.config;

import net.minecraft.command.ICommandSender;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ChatComponentText;

import serverutils.serverlib.lib.gui.IOpenableGui;
import serverutils.serverlib.lib.gui.misc.GuiSelectItemStack;
import serverutils.serverlib.lib.io.DataIn;
import serverutils.serverlib.lib.io.DataOut;
import serverutils.serverlib.lib.item.ItemStackSerializer;
import serverutils.serverlib.lib.util.misc.MouseButton;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ConfigItemStack extends ConfigValue
{
	public static final String ID = "item_stack";

	public static class SimpleStack extends ConfigItemStack
	{
		private final Supplier<ItemStack> get;
		private final Consumer<ItemStack> set;

		public SimpleStack(boolean single, Supplier<ItemStack> g, Consumer<ItemStack> s)
		{
			super(ItemStack.EMPTY, single);
			get = g;
			set = s;
		}

		public SimpleStack(Supplier<ItemStack> g, Consumer<ItemStack> s)
		{
			this(false, g, s);
		}

		@Override
		public ItemStack getStack()
		{
			return get.get();
		}

		@Override
		public void setStack(ItemStack v)
		{
			set.accept(v);
		}
	}

	private ItemStack value;
	private boolean singleItemOnly;

	public ConfigItemStack(ItemStack is, boolean b)
	{
		value = is.isEmpty() ? ItemStack.EMPTY : is;
		singleItemOnly = b;

		if (singleItemOnly && value.stackSize > 1)
		{
			value.stackSize = 1;
		}
	}

	public ConfigItemStack(ItemStack is)
	{
		this(is, false);
	}

	@Override
	public String getId()
	{
		return ID;
	}

	public ItemStack getStack()
	{
		return value;
	}

	public void setStack(ItemStack is)
	{
		value = is.isEmpty() ? ItemStack.EMPTY : is;

		if (getSingleItemOnly() && value.stackSize > 1)
		{
			value.stackSize = 1;
		}
	}

	public boolean getSingleItemOnly()
	{
		return singleItemOnly;
	}

	public void setSingleItemOnly(boolean v)
	{
		singleItemOnly = v;
	}

	@Override
	public String getString()
	{
		return getStack().serializeNBT().toString();
	}

	@Override
	public boolean getBoolean()
	{
		return getInt() > 0;
	}

	@Override
	public int getInt()
	{
		return getStack().stackSize;
	}

	@Override
	public ConfigItemStack copy()
	{
		return new ConfigItemStack(getStack(), getSingleItemOnly());
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String key)
	{
		value = getStack();

		if (!value.isEmpty())
		{
			nbt.setTag(key, value.serializeNBT());
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String key)
	{
		NBTTagCompound nbt1 = nbt.getCompoundTag(key);

		if (nbt1.isEmpty())
		{
			setStack(ItemStack.EMPTY);
		}
		else
		{
			setStack(new ItemStack(nbt1));
		}
	}

	@Override
	public void writeData(DataOut data)
	{
		data.writeItemStack(getStack());
		data.writeBoolean(getSingleItemOnly());
	}

	@Override
	public void readData(DataIn data)
	{
		setStack(data.readItemStack());
		setSingleItemOnly(data.readBoolean());
	}

	@Override
	public boolean isEmpty()
	{
		return getStack().isEmpty();
	}

	@Override
	public IChatComponent getStringForGUI()
	{
		value = getStack();

		if (value.stackSize <= 1)
		{
			return new ChatComponentText(value.getDisplayName());
		}

		return new ChatComponentText(value.stackSize + "x " + value.getDisplayName());
	}

	@Override
	public void onClicked(IOpenableGui gui, ConfigValueInstance inst, MouseButton button, Runnable callback)
	{
		if (inst.getCanEdit())
		{
			new GuiSelectItemStack(gui, getStack().copy(), getSingleItemOnly(), stack -> {
				setStack(stack);
				callback.run();
			}).openGui();
		}
	}

	@Override
	public boolean setValueFromString(@Nullable ICommandSender sender, String string, boolean simulate)
	{
		if (string.isEmpty())
		{
			return false;
		}

		try
		{
			ItemStack stack = ItemStackSerializer.parseItemThrowingException(string);

			if (stack.stackSize > 1 && getSingleItemOnly())
			{
				return false;
			}

			if (!simulate)
			{
				setStack(stack);
			}

			return true;
		}
		catch (Exception ex)
		{
			return false;
		}
	}

	@Override
	public void setValueFromOtherValue(ConfigValue value)
	{
		if (value instanceof ConfigItemStack)
		{
			setStack(((ConfigItemStack) value).getStack().copy());
		}
		else
		{
			super.setValueFromOtherValue(value);
		}
	}
}