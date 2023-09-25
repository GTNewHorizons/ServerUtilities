package serverutils.serverlib.lib.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

import serverutils.serverlib.lib.item.IItemHandler;
import serverutils.serverlib.lib.item.IItemHandlerModifiable;
import serverutils.serverlib.lib.item.ItemHandlerHelper;

public class InvUtils {

	public static final int BUCKET_VOLUME = 1000;
	public static final ItemStack EMPTY_STACK = null;
	public static final IInventory EMPTY_INVENTORY = new InventoryBasic("[Null]", true, 0);
	public static final Predicate<ItemStack> NO_FILTER = stack -> true;

	// @GameRegistry.ObjectHolder("itemfilters:missing")
	// public static Item missingItem;

	public static final ItemStack brokenItem(String id) {
		// if (missingItem != null) {
		// ItemStack stack = new ItemStack(missingItem);
		// stack.setTagInfo("item", new NBTTagString(id));
		// return stack;
		// }

		ItemStack stack = new ItemStack(Items.fish);
		stack.setStackDisplayName("Broken Item with ID " + id);
		return stack;
	}

	@Nullable
	public static NBTTagCompound nullIfEmpty(@Nullable NBTTagCompound nbt) {
		return nbt == null || nbt.hasNoTags() ? null : nbt;
	}

	public static void dropItem(World w, double x, double y, double z, double mx, double my, double mz, ItemStack item,
								int delay) {
		if (item != null) {
			EntityItem ei = new EntityItem(w, x, y, z, item.copy());
			ei.motionX = mx;
			ei.motionY = my;
			ei.motionZ = mz;
			ei.delayBeforeCanPickup = delay;
			w.spawnEntityInWorld(ei);
		}
	}

	public static void dropItem(World w, double x, double y, double z, ItemStack item, int delay) {
		dropItem(
				w,
				x,
				y,
				z,
				w.rand.nextGaussian() * 0.07F,
				w.rand.nextFloat() * 0.05F,
				w.rand.nextGaussian() * 0.07F,
				item,
				delay);
	}

	public static void dropItem(World w, int posx, int posy, int posz, ItemStack item, int delay) {
		dropItem(w, posx + 0.5D, posy + 0.5D, posz + 0.5D, item, delay);
	}

	public static void dropItem(Entity e, ItemStack item) {
		dropItem(e.worldObj, e.posX, e.posY, e.posZ, item, 0);
	}

	public static void dropAllItems(World world, double x, double y, double z, Iterable<ItemStack> items) {
		if (!world.isRemote) {
			for (ItemStack item : items) {
				if (item != null) {
					dropItem(world, x, y, z, item, 10);
				}
			}
		}
	}

	public static void dropAllItems(World world, double x, double y, double z, @Nullable IItemHandler itemHandler) {
		if (!world.isRemote && itemHandler != null && itemHandler.getSlots() > 0) {
			for (int i = 0; i < itemHandler.getSlots(); i++) {
				ItemStack item = itemHandler.getStackInSlot(i);

				if (item != null) {
					dropItem(world, x, y, z, item, 10);
				}
			}
		}
	}

	public static void transferItems(@Nullable IItemHandler from, @Nullable IItemHandler to, int amount,
									 Predicate<ItemStack> filter) {
		if (amount <= 0 || from == null || to == null) {
			return;
		}

		for (int i = 0; i < from.getSlots(); i++) {
			ItemStack extracted = from.extractItem(i, amount, true);

			if (extracted != null && filter.test(extracted)) {
				ItemStack inserted = ItemHandlerHelper.insertItem(to, extracted, false);
				int s = extracted.stackSize - inserted.stackSize;

				if (s > 0) {
					from.extractItem(i, s, false);
					amount -= s;

					if (amount <= 0) {
						return;
					}
				}
			}
		}
	}

	public static void writeItemHandler(NBTTagCompound nbt, String key, IItemHandlerModifiable itemHandler) {
		NBTTagList list = new NBTTagList();

		int slots = itemHandler.getSlots();
		for (int i = 0; i < slots; i++) {
			ItemStack stack = itemHandler.getStackInSlot(i);

			if (stack != null) {
				NBTTagCompound nbt1 = stack.writeToNBT(new NBTTagCompound());
				nbt1.setInteger("Slot", i);
				list.appendTag(nbt1);
			}
		}

		if (list.tagCount() != 0) {
			nbt.setTag(key, list);
		}
	}

	public static boolean stacksAreEqual(ItemStack stackA, ItemStack stackB) {
		return stackA.getItem() == stackB.getItem() && stackA.getItemDamage() == stackB.getItemDamage()
				&& ItemStack.areItemStackTagsEqual(stackA, stackB);
	}

	public static Set<String> getOreNames(@Nullable Set<String> l, ItemStack is) {
		if (is == null) {
			return l == null ? Collections.emptySet() : l;
		}

		int[] ai = OreDictionary.getOreIDs(is);

		if (ai.length > 0) {
			if (l == null) {
				l = new HashSet<>(ai.length);
			}

			for (int i : ai) {
				l.add(OreDictionary.getOreName(i));
			}

			return l;
		}

		return Collections.emptySet();
	}

	public static void forceUpdate(Container container) {
		for (int i = 0; i < container.inventorySlots.size(); ++i) {
			ItemStack itemstack = ((Slot) container.inventorySlots.get(i)).getStack();
			ItemStack itemstack1 = itemstack == null ? InvUtils.EMPTY_STACK : itemstack.copy();
			container.inventoryItemStacks.set(i, itemstack1);

			// for (IContainerListener listener : ATHelper.getContainerListeners(container)) {
			// listener.sendSlotContents(container, i, itemstack1);
			// }
		}
	}

	public static void forceUpdate(EntityPlayer player) {
		forceUpdate(player.inventoryContainer);
	}
}