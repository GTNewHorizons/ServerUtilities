package serverutils.serverlib.lib.item;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.oredict.OreDictionary;
import serverutils.serverlib.lib.math.MathHelper;

import javax.annotation.Nullable;

public class ItemStackSerializer
{
	public static ItemStack parseItemThrowingException(String input) throws Exception
	{
		input = input.trim();
		if (input.isEmpty() || input.equals("-") || input.equals("minecraft:air"))
		{
			return ItemStack.EMPTY;
		}
		else if (input.startsWith("{"))
		{
			NBTTagCompound nbt = (NBTTagCompound) JsonToNBT.func_150315_a(input); //.getTagFromJson(input);

			if (nbt.getByte("Count") <= 0)
			{
				nbt.setByte("Count", (byte) 1);
			}

			return new ItemStack(nbt);
		}

		String[] s1 = input.split(" ", 4);
		Item item = (Item) Item.itemRegistry.getObject(new ResourceLocation(s1[0]));

		if (item == null)
		{
			throw new NullPointerException("Unknown item: " + s1[0]);
		}
		else if (item == Items.AIR)
		{
			return ItemStack.EMPTY;
		}

		int stackSize = 1, meta = 0;

		if (s1.length >= 2)
		{
			stackSize = MathHelper.getInt(s1[1], 1);
		}

		if (s1.length >= 3)
		{
			meta = (s1[2].charAt(0) == '*') ? OreDictionary.WILDCARD_VALUE : MathHelper.getInt(s1[2], 0);
		}

		ItemStack itemstack = new ItemStack(item, stackSize, meta);

		if (s1.length >= 4)
		{
			itemstack.setTagCompound((NBTTagCompound) JsonToNBT.func_150315_a(s1[3]));
		}

		return itemstack.isEmpty() ? ItemStack.EMPTY : itemstack;
	}

	public static ItemStack parseItem(String input)
	{
		try
		{
			return parseItemThrowingException(input);
		}
		catch (Exception ex)
		{
			return ItemStack.EMPTY;
		}
	}

	public static String toString(ItemStack stack)
	{
		if (stack.isEmpty())
		{
			return "minecraft:air";
		}

		NBTTagCompound nbt = stack.serializeNBT();

		if (nbt.hasKey("ForgeCaps"))
		{
			return nbt.toString();
		}

		StringBuilder builder = new StringBuilder(String.valueOf(Item.itemRegistry.getNameForObject(stack.getItem())));

		int count = stack.stackSize;
		int meta = stack.getItemDamage();
		NBTTagCompound tag = stack.getTagCompound();

		if (count > 1 || meta != 0 || tag != null)
		{
			builder.append(' ');
			builder.append(count);
		}

		if (meta != 0 || tag != null)
		{
			builder.append(' ');
			builder.append(meta);
		}

		if (tag != null)
		{
			builder.append(' ');
			builder.append(tag);
		}

		return builder.toString();
	}

	public static NBTBase write(ItemStack stack, boolean forceCompound)
	{
		if (stack.isEmpty())
		{
			return forceCompound ? new NBTTagCompound() : new NBTTagString("");
		}

		NBTTagCompound nbt = stack.serializeNBT();

		if (!nbt.hasKey("ForgeCaps") && !nbt.hasKey("tag"))
		{
			if (!forceCompound)
			{
				return new NBTTagString(toString(stack));
			}

			NBTTagCompound nbt1 = new NBTTagCompound();
			nbt1.setString("item", toString(stack));
			return nbt1;
		}

		if (nbt.getByte("Count") == 1)
		{
			nbt.removeTag("Count");
		}

		if (nbt.getShort("Damage") == 0)
		{
			nbt.removeTag("Damage");
		}

		return nbt;
	}

	public static ItemStack read(@Nullable NBTBase nbtBase)
	{
		if (nbtBase == null || nbtBase.isEmpty())
		{
			return ItemStack.EMPTY;
		}
		else if (nbtBase instanceof NBTTagString)
		{
			return parseItem(((NBTTagString) nbtBase).func_150285_a_());
		}
		else if (!(nbtBase instanceof NBTTagCompound))
		{
			return ItemStack.EMPTY;
		}

		NBTTagCompound nbt = (NBTTagCompound) nbtBase;

		if (nbt.hasKey("item", Constants.NBT.TAG_STRING))
		{
			return parseItem(nbt.getString("item"));
		}

		if (!nbt.hasKey("Count"))
		{
			nbt.setByte("Count", (byte) 1);
		}

		ItemStack stack = new ItemStack(nbt);
		return stack.isEmpty() ? ItemStack.EMPTY : stack;
	}
}