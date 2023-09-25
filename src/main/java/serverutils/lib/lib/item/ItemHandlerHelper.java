package serverutils.lib.lib.item;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;

public class ItemHandlerHelper {

    public static ItemStack insertItem(IItemHandler dest, ItemStack stack, boolean simulate) {
        if (dest == null || stack == null) return stack;

        for (int i = 0; i < dest.getSlots(); i++) {
            stack = dest.insertItem(i, stack, simulate);
        }

        return stack;
    }

    public static boolean canItemStacksStack(ItemStack a, ItemStack b) {
        if (a == null || !a.isItemEqual(b) || a.hasTagCompound() != b.hasTagCompound()) return false;

        return (!a.hasTagCompound() || a.getTagCompound().equals(b.getTagCompound()));
    }

    /**
     * A relaxed version of canItemStacksStack that stacks itemstacks with different metadata if they don't have
     * subtypes. This usually only applies when players pick up items.
     */
    public static boolean canItemStacksStackRelaxed(ItemStack a, ItemStack b) {
        if (a == null || b == null || a.getItem() != b.getItem()) return false;

        if (!a.isStackable()) return false;

        // Metadata value only matters when the item has subtypes
        // Vanilla stacks non-subtype items with different metadata together
        // e.g. a stick with metadata 0 and a stick with metadata 1 stack
        if (a.getHasSubtypes() && a.getItemDamage() != b.getItemDamage()) return false;

        if (a.hasTagCompound() != b.hasTagCompound()) return false;

        return (!a.hasTagCompound() || a.getTagCompound().equals(b.getTagCompound()));
    }

    public static ItemStack copyStackWithSize(ItemStack itemStack, int size) {
        if (size == 0) return null;
        ItemStack copy = itemStack.copy();
        copy.stackSize = size;
        return copy;
    }

    /**
     * Inserts the ItemStack into the inventory, filling up already present stacks first. This is equivalent to the
     * behaviour of a player picking up an item. Note: This function stacks items without subtypes with different
     * metadata together.
     */
    public static ItemStack insertItemStacked(IItemHandler inventory, ItemStack stack, boolean simulate) {
        if (inventory == null || stack == null) return stack;

        // not stackable -> just insert into a new slot
        if (!stack.isStackable()) {
            return insertItem(inventory, stack, simulate);
        }

        int sizeInventory = inventory.getSlots();

        // go through the inventory and try to fill up already existing items
        for (int i = 0; i < sizeInventory; i++) {
            ItemStack slot = inventory.getStackInSlot(i);
            if (canItemStacksStackRelaxed(slot, stack)) {
                stack = inventory.insertItem(i, stack, simulate);

                if (stack == null) {
                    break;
                }
            }
        }

        // insert remainder into empty slots
        if (stack != null) {
            // find empty slot
            for (int i = 0; i < sizeInventory; i++) {
                if (inventory.getStackInSlot(i) == null) {
                    stack = inventory.insertItem(i, stack, simulate);
                    if (stack == null) {
                        break;
                    }
                }
            }
        }

        return stack;
    }

    // /** giveItemToPlayer without preferred slot */
    // public static void giveItemToPlayer(EntityPlayer player, @Nonnull ItemStack stack) {
    // giveItemToPlayer(player, stack, -1);
    // }

    // /**
    // * Inserts the given itemstack into the players inventory.
    // * If the inventory can't hold it, the item will be dropped in the world at the
    // * players position.
    // *
    // * @param player The player to give the item to
    // * @param stack The itemstack to insert
    // */
    // public static void giveItemToPlayer(EntityPlayer player, @Nonnull ItemStack stack, int preferredSlot) {
    // if (stack == null)
    // return;

    // IItemHandler inventory = new PlayerMainInvWrapper(player.inventory);
    // World world = player.worldObj;

    // // try adding it into the inventory
    // ItemStack remainder = stack;
    // // insert into preferred slot first
    // if (preferredSlot >= 0 && preferredSlot < inventory.getSlots()) {
    // remainder = inventory.insertItem(preferredSlot, stack, false);
    // }
    // // then into the inventory in general
    // if (remainder != null) {
    // remainder = insertItemStacked(inventory, remainder, false);
    // }

    // // play sound if something got picked up
    // if (remainder == null || remainder.stackSize != stack.stackSize) {
    // world.playSound(null, player.posX, player.posY + 0.5, player.posZ,
    // SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2F,
    // ((world.rand.nextFloat() - world.rand.nextFloat()) * 0.7F + 1.0F) * 2.0F);
    // }

    // // drop remaining itemstack into the world
    // if (remainder != null && !world.isRemote) {
    // EntityItem entityitem = new EntityItem(world, player.posX, player.posY + 0.5, player.posZ, remainder);
    // entityitem.delayBeforeCanPickup = 40;
    // entityitem.motionX = 0;
    // entityitem.motionZ = 0;

    // world.spawnEntityInWorld(entityitem);
    // }
    // }

    /**
     * This method uses the standard vanilla algorithm to calculate a comparator output for how "full" the inventory is.
     * This method is an adaptation of Container#calcRedstoneFromInventory(IInventory).
     * 
     * @param inv The inventory handler to test.
     * @return A redstone value in the range [0,15] representing how "full" this inventory is.
     */
    public static int calcRedstoneFromInventory(@Nullable IItemHandler inv) {
        if (inv == null) {
            return 0;
        } else {
            int itemsFound = 0;
            float proportion = 0.0F;

            for (int j = 0; j < inv.getSlots(); ++j) {
                ItemStack itemstack = inv.getStackInSlot(j);

                if (itemstack != null) {
                    proportion += (float) itemstack.stackSize
                            / (float) Math.min(inv.getSlotLimit(j), itemstack.getMaxStackSize());
                    ++itemsFound;
                }
            }

            proportion = proportion / (float) inv.getSlots();
            return MathHelper.floor_float(proportion * 14.0F) + (itemsFound > 0 ? 1 : 0);
        }
    }
}
