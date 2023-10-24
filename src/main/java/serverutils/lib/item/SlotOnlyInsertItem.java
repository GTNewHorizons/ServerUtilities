package serverutils.lib.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import serverutils.lib.util.InvUtils;

public class SlotOnlyInsertItem extends Slot {

    private final IItemHandler itemHandler;

    public SlotOnlyInsertItem(IItemHandler i, int index, int xPosition, int yPosition) {
        super(InvUtils.EMPTY_INVENTORY, index, xPosition, yPosition);
        itemHandler = i;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return itemHandler.insertItem(0, stack, true) != stack;
    }

    @Override
    public ItemStack getStack() {
        return InvUtils.EMPTY_STACK;
    }

    @Override
    public void putStack(ItemStack stack) {
        if (itemHandler.insertItem(getSlotIndex(), stack, false) != stack) {
            onSlotChanged();
        }
    }

    // @Override
    // public ItemStack onTake(EntityPlayer thePlayer, ItemStack stack) {
    // return stack;
    // }

    @Override
    public void onSlotChange(ItemStack stack1, ItemStack stack2) {}

    @Override
    public int getSlotStackLimit() {
        return itemHandler.getSlotLimit(getSlotIndex());
    }

    @Override
    public boolean canTakeStack(EntityPlayer playerIn) {
        return false;
    }

    @Override
    public ItemStack decrStackSize(int amount) {
        return InvUtils.EMPTY_STACK;
    }

    // @Override
    // public boolean isSameInventory(Slot other) {
    // return other instanceof SlotOnlyInsertItem && ((SlotOnlyInsertItem) other).itemHandler == itemHandler;
    // }
}
