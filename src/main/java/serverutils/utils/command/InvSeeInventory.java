package serverutils.utils.command;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

public class InvSeeInventory implements IInventory {

    private final IInventory inventory;
    private final EntityPlayerMP player;

    public InvSeeInventory(IInventory inv, @Nullable EntityPlayerMP ep) {
        inventory = inv;
        player = ep;
    }

    @Override
    public int getSizeInventory() {
        return 45;
    }

    // @Override
    // public boolean isEmpty() {
    // return inventory.isEmpty();
    // }

    public int getSlot(int slot) {
        if (slot == 8) {
            return 40;
        } else if (slot >= 0 && slot <= 3) {
            return 39 - slot;
        } else if (slot >= 9 && slot <= 35) {
            return slot;
        } else if (slot >= 36 && slot <= 44) {
            return slot - 36;
        }

        return -1;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        int slot = getSlot(index);
        return slot == -1 ? null : inventory.getStackInSlot(slot);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        int slot = getSlot(index);
        return slot == -1 ? null : inventory.decrStackSize(slot, count);
    }

    // public ItemStack removeStackFromSlot(int index) {
    // int slot = getSlot(index);
    // return slot == -1 ? null : inventory.removeStackFromSlot(slot);
    // }

    @Override
    public void setInventorySlotContents(int index, ItemStack is) {
        int slot = getSlot(index);

        if (slot != -1) {
            inventory.setInventorySlotContents(slot, is);
            markDirty();
        }
    }

    @Override
    public String getInventoryName() {
        return inventory.getInventoryName();
    }

    @Override
    public boolean hasCustomInventoryName() {
        return true;
    }

    @Override
    public void closeInventory() {
        // TODO Auto-generated method stub

    }

    @Override
    public ItemStack getStackInSlotOnClosing(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void openInventory() {
        // TODO Auto-generated method stub

    }

    public IChatComponent getDisplayName() {
        if (player != null) {
            return new ChatComponentText(player.getDisplayName());
        }

        return new ChatComponentText(inventory.getInventoryName());
    }

    @Override
    public int getInventoryStackLimit() {
        return inventory.getInventoryStackLimit();
    }

    @Override
    public void markDirty() {
        inventory.markDirty();

        if (player != null) {
            player.openContainer.detectAndSendChanges();
        }
    }

    public boolean isUsableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        int slot = getSlot(index);
        return slot != -1 && inventory.isItemValidForSlot(slot, stack);
    }

    public int getField(int id) {
        return 0;
    }

    public void setField(int id, int value) {}

    public int getFieldCount() {
        return 0;
    }

    // public void clear() {
    // inventory.clear();
    // }
}
