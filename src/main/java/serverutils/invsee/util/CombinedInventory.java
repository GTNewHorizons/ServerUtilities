package serverutils.invsee.util;

import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Range;
import com.google.common.collect.TreeRangeMap;

@SuppressWarnings("UnstableApiUsage")
public class CombinedInventory implements IInventory {

    private final TreeRangeMap<Integer, IInventory> inventoryRanges = TreeRangeMap.create();
    private final Map<Range<Integer>, IInventory> inventoryMap = inventoryRanges.asMapOfRanges();
    private IInventory activeInventory;
    private int size;

    public void addInventory(IInventory inventory, Range<Integer> slotRange) {
        size += slotRange.upperEndpoint() - slotRange.lowerEndpoint();
        inventoryRanges.put(slotRange, inventory);
    }

    public void addInventories(IInventory... inventories) {
        for (IInventory inventory : inventories) {
            addInventory(inventory);
        }
    }

    public void addInventory(IInventory inventory) {
        Range<Integer> slotRange = Range.closedOpen(size, size + inventory.getSizeInventory());
        size += inventory.getSizeInventory();
        inventoryRanges.put(slotRange, inventory);
    }

    public @Nullable <T extends IInventory> T getInventory(Class<T> inventoryClass) {
        for (IInventory inventory : inventoryMap.values()) {
            if (inventoryClass.isInstance(inventory)) {
                return inventoryClass.cast(inventory);
            }
        }
        return null;
    }

    public @Nullable <T extends IInventory> T getInventory(Class<T> inventoryClass, int slot) {
        IInventory inventory = inventoryRanges.get(slot);
        return inventoryClass.isInstance(inventory) ? inventoryClass.cast(inventory) : null;
    }

    @Override
    public int getSizeInventory() {
        return size;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        Map.Entry<Range<Integer>, IInventory> entry = inventoryRanges.getEntry(index);
        if (entry != null) {
            activeInventory = entry.getValue();
            return activeInventory.getStackInSlot(index - entry.getKey().lowerEndpoint());
        }
        return null;
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        Map.Entry<Range<Integer>, IInventory> entry = inventoryRanges.getEntry(index);
        if (entry != null) {
            activeInventory = entry.getValue();
            return activeInventory.decrStackSize(index - entry.getKey().lowerEndpoint(), count);
        }
        return null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int index) {
        Map.Entry<Range<Integer>, IInventory> entry = inventoryRanges.getEntry(index);
        if (entry != null) {
            activeInventory = entry.getValue();
            return activeInventory.getStackInSlotOnClosing(index - entry.getKey().lowerEndpoint());
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        Map.Entry<Range<Integer>, IInventory> entry = inventoryRanges.getEntry(index);
        if (entry != null) {
            activeInventory = entry.getValue();
            activeInventory.setInventorySlotContents(index - entry.getKey().lowerEndpoint(), stack);
        }
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        Map.Entry<Range<Integer>, IInventory> entry = inventoryRanges.getEntry(index);
        if (entry != null) {
            activeInventory = entry.getValue();
            return activeInventory.isItemValidForSlot(index - entry.getKey().lowerEndpoint(), stack);
        }
        return false;
    }

    @Override
    public String getInventoryName() {
        return "";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return activeInventory == null ? 64 : activeInventory.getInventoryStackLimit();
    }

    @Override
    public void markDirty() {
        if (activeInventory != null) {
            activeInventory.markDirty();
        }
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}
}
