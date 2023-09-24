package serverutils.serverlib.lib.item;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fe. items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.Arrays;

public class ItemStackArrayHandler implements IItemHandlerModifiable
{
	public final ItemStack[] items;

	public ItemStackArrayHandler(int size)
	{
		items = new ItemStack[size];
		Arrays.fill(items, ItemStack.EMPTY);
	}

	@Override
	public int getSlots()
	{
		return items.length;
	}

	@Override
	public ItemStack getStackInSlot(int slot)
	{
		return items[slot];
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack)
	{
		items[slot] = stack;
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
	{
		if (stack.isEmpty())
		{
			return ItemStack.EMPTY;
		}

		if (!isItemValid(slot, stack))
		{
			return stack;
		}

		ItemStack existing = items[slot];

		int limit = getStackLimit(slot, stack);

		if (!existing.isEmpty())
		{
			if (!ItemHandlerHelper.canItemStacksStack(stack, existing))
			{
				return stack;
			}

			limit -= existing.getCount();
		}

		if (limit <= 0)
		{
			return stack;
		}

		boolean reachedLimit = stack.getCount() > limit;

		if (!simulate)
		{
			if (existing.isEmpty())
			{
				items[slot] = reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, limit) : stack;
			}
			else
			{
				existing.grow(reachedLimit ? limit : stack.getCount());
			}

			markDirty(slot);
		}

		return reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - limit) : ItemStack.EMPTY;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate)
	{
		if (amount == 0)
		{
			return ItemStack.EMPTY;
		}

		ItemStack existing = items[slot];

		if (existing.isEmpty())
		{
			return ItemStack.EMPTY;
		}

		int toExtract = Math.min(amount, existing.getMaxStackSize());

		if (existing.getCount() <= toExtract)
		{
			if (!simulate)
			{
				items[slot] = ItemStack.EMPTY;
				markDirty(slot);
			}

			return existing;
		}

		if (!simulate)
		{
			items[slot] = ItemHandlerHelper.copyStackWithSize(existing, existing.getCount() - toExtract);
			markDirty(slot);
		}

		return ItemHandlerHelper.copyStackWithSize(existing, toExtract);
	}

	protected int getStackLimit(int slot, ItemStack stack)
	{
		return Math.min(getSlotLimit(slot), stack.getMaxStackSize());
	}

	@Override
	public int getSlotLimit(int slot)
	{
		return 64;
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack)
	{
		return true;
	}

	public void markDirty(int slot)
	{
	}
}