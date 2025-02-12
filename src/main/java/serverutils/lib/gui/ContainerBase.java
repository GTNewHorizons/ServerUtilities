package serverutils.lib.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import serverutils.lib.util.InvUtils;

public abstract class ContainerBase extends Container {

    public final EntityPlayer player;

    public ContainerBase(EntityPlayer ep) {
        player = ep;
    }

    public abstract int getNonPlayerSlots();

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    public void addPlayerSlots(int posX, int posY, boolean ignoreCurrent) {
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlotToContainer(new Slot(player.inventory, x + y * 9 + 9, posX + x * 18, posY + y * 18));
            }
        }

        int i = ignoreCurrent ? player.inventory.currentItem : -1;

        for (int x = 0; x < 9; x++) {
            if (x != i) {
                addSlotToContainer(new Slot(player.inventory, x, posX + x * 18, posY + 58));
            } else {
                addSlotToContainer(new Slot(player.inventory, x, posX + x * 18, posY + 58) {

                    @Override
                    public boolean canTakeStack(EntityPlayer ep) {
                        return false;
                    }
                });
            }
        }
    }

    public void addPlayerSlots(int posX, int posY) {
        addPlayerSlots(posX, posY, false);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        int nonPlayerSlots = getNonPlayerSlots();

        if (nonPlayerSlots <= 0) {
            return InvUtils.EMPTY_STACK;
        }

        ItemStack stack = InvUtils.EMPTY_STACK;
        Slot slot = inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack stack1 = slot.getStack();
            stack = stack1.copy();

            if (index < nonPlayerSlots) {
                if (!mergeItemStack(stack1, nonPlayerSlots, inventorySlots.size(), true)) {
                    return InvUtils.EMPTY_STACK;
                }
            } else if (!mergeItemStack(stack1, 0, nonPlayerSlots, false)) {
                return InvUtils.EMPTY_STACK;
            }

            if (stack1.stackSize == 0) {
                slot.putStack(InvUtils.EMPTY_STACK);
            } else {
                slot.onSlotChanged();
            }
        }

        return stack;
    }
}
