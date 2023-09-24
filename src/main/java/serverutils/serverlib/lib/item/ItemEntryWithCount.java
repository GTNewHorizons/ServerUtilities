package serverutils.serverlib.lib.item;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;

public class ItemEntryWithCount
{
	public static final ItemEntryWithCount EMPTY = new ItemEntryWithCount(ItemEntry.EMPTY, 0)
	{
		@Override
		public boolean isEmpty()
		{
			return true;
		}
	};

	public final ItemEntry entry;
	public int count;

	public ItemEntryWithCount(ItemEntry e, int c)
	{
		entry = e;
		count = c;
	}

	public ItemEntryWithCount(NBTBase nbtb)
	{
		NBTTagCompound nbt = nbtb instanceof NBTTagCompound ? (NBTTagCompound) nbtb : null;
		NBTBase id = nbt == null ? null : nbt.getTag("I");

		if (id == null)
		{
			ItemStack stack = ItemStackSerializer.read(nbtb);
			entry = ItemEntry.get(stack);
			count = nbt != null && nbt.hasKey("RealCount") ? nbt.getInteger("RealCount") : stack.stackSize;
			return;
		}

		if (id instanceof NBTTagIntArray)
		{
			ItemEntry e = ItemEntry.EMPTY;
			int[] ai = ((NBTTagIntArray) id).func_150302_c(); //getIntArray();

			if (ai.length > 0)
			{
				Item item = (Item) Item.itemRegistry.getObjectById(ai[0]);

				if (item != null && item != Items.AIR)
				{
					count = 1;
					int meta = 0;

					if (ai.length >= 2)
					{
						count = ai[1];
					}

					if (ai.length >= 3)
					{
						meta = ai[2];
					}

					NBTTagCompound tag = (NBTTagCompound) nbt.getTag("N");
					NBTTagCompound caps = (NBTTagCompound) nbt.getTag("C");
					e = new ItemEntry(item, meta, tag, caps);
				}
			}

			entry = e;
			return;
		}

		Item item = id instanceof NBTTagString ? (Item) Item.itemRegistry.getObject(new ResourceLocation(((NBTTagString) id).func_150285_a_())) : (Item) Item.itemRegistry.getObjectById(((NBTBase.NBTPrimitive) id).func_150287_d());

		if (item != null && item != Items.AIR)
		{
			int meta = nbt.getShort("M");
			NBTTagCompound tag = (NBTTagCompound) nbt.getTag("N");
			NBTTagCompound caps = (NBTTagCompound) nbt.getTag("C");
			entry = new ItemEntry(item, meta, tag, caps);
			count = nbt.getInteger("S");
			count = count <= 0 ? 1 : count;
		}
		else
		{
			entry = ItemEntry.EMPTY;
			count = 0;
		}
	}

	public boolean isEmpty()
	{
		return count <= 0 || entry.isEmpty();
	}

	public NBTTagCompound serializeNBT()
	{
		NBTTagCompound nbt = new NBTTagCompound();

		if (isEmpty())
		{
			return nbt;
		}

		if (count > 1 || entry.metadata > 0)
		{
			int[] ai = new int[entry.metadata > 0 ? 3 : 2];
			ai[0] = Item.itemRegistry.getIDForObject(entry.item);
			ai[1] = count;

			if (entry.metadata > 0)
			{
				ai[2] = entry.metadata;
			}

			nbt.setIntArray("I", ai);
		}
		else
		{
			nbt.setInteger("I", Item.itemRegistry.getIDForObject(entry.item));
		}

		if (entry.nbt != null)
		{
			nbt.setTag("N", entry.nbt);
		}

		if (entry.caps != null)
		{
			nbt.setTag("C", entry.caps);
		}

		return nbt;
	}

	public ItemStack getStack(boolean copy)
	{
		return entry.getStack(count, copy);
	}

	public String toString()
	{
		return serializeNBT().toString();
	}
}