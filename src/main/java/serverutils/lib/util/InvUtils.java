package serverutils.lib.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

import serverutils.lib.ATHelper;

public class InvUtils {

    public static final int BUCKET_VOLUME = 1000;
    public static final ItemStack EMPTY_STACK = null;
    public static final IInventory EMPTY_INVENTORY = new InventoryBasic("[Null]", true, 0);
    public static final Predicate<ItemStack> NO_FILTER = stack -> true;

    public static ItemStack brokenItem(String id) {
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

    public static void dropAllItems(World world, double x, double y, double z, @Nullable IInventory inventory) {
        if (!world.isRemote && inventory != null && inventory.getSizeInventory() > 0) {
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                ItemStack item = inventory.getStackInSlot(i);

                if (item != null) {
                    dropItem(world, x, y, z, item, 10);
                }
            }
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

    public static int[] getPlayerSlots(EntityPlayer player) {
        return IntStream.range(0, player.inventory.mainInventory.length).toArray();
    }

    public static ItemStack singleCopy(ItemStack is) {
        if (is.stackSize <= 0) return null;
        ItemStack is1 = is.copy();
        is1.stackSize = 1;
        return is1;
    }

    public static boolean addSingleItemToInv(ItemStack is, IInventory inv, int[] slots, boolean doAdd) {
        ItemStack single = singleCopy(is);
        if (single == null) return false;

        for (int slot : slots) {
            ItemStack is1 = inv.getStackInSlot(slot);
            if (is1 != null && is1.stackSize > 0 && stacksAreEqual(is, is1)) {
                if (is1.stackSize + 1 <= is1.getMaxStackSize()) {
                    if (doAdd) {
                        is1.stackSize++;
                        inv.setInventorySlotContents(slot, is1);
                        inv.markDirty();
                    }

                    return true;
                }
            }
        }
        for (int slot2 : slots) {
            ItemStack is1 = inv.getStackInSlot(slot2);
            if (is1 == null || is1.stackSize == 0) {
                if (doAdd) {
                    inv.setInventorySlotContents(slot2, single);
                    inv.markDirty();
                }

                return true;
            }
        }

        return false;
    }

    public static void giveItem(EntityPlayer player, ItemStack item) {
        if (player.inventory == null || item.stackSize <= 0) return;
        ItemStack is = item.copy();
        boolean changed = false;

        int size = is.stackSize;
        for (int i = 0; i < size; i++) {
            if (addSingleItemToInv(is, player.inventory, getPlayerSlots(player), true)) {
                is.stackSize--;
                changed = true;
            }
        }

        if (changed) {
            player.inventory.markDirty();
            if (player.openContainer != null) player.openContainer.detectAndSendChanges();
        }

        if (is.stackSize > 0) dropItem(player, is);
    }

    public static void giveItemFromIterable(EntityPlayer player, Iterable<ItemStack> items) {
        for (ItemStack item : items) {
            if (item != null) {
                giveItem(player, item);
            }
        }
    }

    public static void forceUpdate(Container container) {
        for (int i = 0; i < container.inventorySlots.size(); ++i) {
            ItemStack itemstack = container.inventorySlots.get(i).getStack();
            ItemStack itemstack1 = itemstack == null ? InvUtils.EMPTY_STACK : itemstack.copy();
            container.inventoryItemStacks.set(i, itemstack1);

            for (ICrafting listener : ATHelper.getContainerListeners(container)) {
                listener.sendSlotContents(container, i, itemstack1);
            }
        }
    }

    public static void forceUpdate(EntityPlayer player) {
        forceUpdate(player.inventoryContainer);
    }

    public static boolean clear(IInventory inv) {
        if (inv == null) return false;
        boolean hadItems = false;

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack is = removeStackFromSlot(inv, i);
            if (!hadItems && is != null && is.stackSize > 0) hadItems = true;
        }

        if (hadItems) inv.markDirty();
        return hadItems;
    }

    public static ItemStack removeStackFromSlot(IInventory inv, int i) {
        if (inv == null) return null;
        ItemStack is = inv.getStackInSlot(i);

        if (is != null) {
            inv.setInventorySlotContents(i, null);
            return (is.stackSize > 0) ? is : null;
        }

        return null;
    }
}
