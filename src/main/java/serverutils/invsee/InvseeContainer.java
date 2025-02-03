package serverutils.invsee;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import serverutils.invsee.inventories.IModdedInventory;
import serverutils.invsee.inventories.InvSeeInventories;
import serverutils.lib.data.ForgePlayer;

public class InvseeContainer extends Container {

    private final Map<InvSeeInventories, IInventory> inventories;
    private final EntityPlayer viewingPlayer;
    private final ForgePlayer otherPlayer;
    private final Map<InvSeeInventories, List<Slot>> moddedInventorySlots = new HashMap<>();
    private final Set<InvSeeInventories> modifiedInventories = new HashSet<>();
    private InvSeeInventories activeInventory;

    public InvseeContainer(Map<InvSeeInventories, IInventory> moddedInventories, EntityPlayer player,
            @Nullable ForgePlayer otherPlayer) {
        this.inventories = moddedInventories;
        this.viewingPlayer = player;
        this.otherPlayer = otherPlayer;

        for (Map.Entry<InvSeeInventories, IInventory> entry : moddedInventories.entrySet()) {
            IModdedInventory moddedInventory = entry.getKey().getNullableInventory();
            if (moddedInventory == null) continue;
            IInventory inventory = entry.getValue();

            List<Slot> inventorySlots = moddedInventorySlots
                    .computeIfAbsent(entry.getKey(), a -> new ArrayList<>(inventory.getSizeInventory()));
            int slotsInRow = 0;
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                if (slotsInRow == 9) slotsInRow = 0;
                Slot slot = moddedInventory.getSlot(player, inventory, i, 8 + slotsInRow++ * 18, 54 - (i / 9) * 18);
                if (slot != null) {
                    inventorySlots.add(slot);
                } else if (slotsInRow > 0) {
                    slotsInRow--;
                }
            }

            inventorySlots.sort(Comparator.comparingInt(Slot::getSlotIndex));
        }

        setActiveInventory(InvSeeInventories.MAIN);
    }

    public void setActiveInventory(InvSeeInventories inventory) {
        activeInventory = inventory;
        inventorySlots.clear();
        for (Slot slot : moddedInventorySlots.get(inventory)) {
            addSlotToContainer(slot);
        }

        for (int j = 0; j < 3; ++j) {
            for (int k = 0; k < 9; ++k) {
                this.addSlotToContainer(
                        new Slot(viewingPlayer.inventory, k + j * 9 + 9, 8 + k * 18, 103 - 18 + j * 18));
            }
        }

        for (int j = 0; j < 9; ++j) {
            this.addSlotToContainer(new Slot(viewingPlayer.inventory, j, 8 + j * 18, 161 - 18));
        }

        detectAndSendChanges();
    }

    public InvSeeInventories getActiveInventory() {
        return activeInventory;
    }

    public int getHighestSlot() {
        return inventorySlots.stream().mapToInt(e -> e.yDisplayPosition).min().orElse(0);
    }

    public int getLowestSlot() {
        return inventorySlots.stream().mapToInt(e -> e.yDisplayPosition).max().orElse(0);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        if (!viewingPlayer.worldObj.isRemote && otherPlayer != null && otherPlayer.isOnline()) {
            Container container = otherPlayer.getPlayer().openContainer;
            if (!container.crafters.contains((EntityPlayerMP) viewingPlayer)) {
                container.detectAndSendChanges();
            }
        }
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);

        if (!player.worldObj.isRemote && otherPlayer != null) {
            List<InvSeeInventories> modifiedInventories = new ArrayList<>(this.modifiedInventories);
            modifiedInventories.sort(Comparator.comparingInt(InvSeeInventories::ordinal));
            for (InvSeeInventories inventory : modifiedInventories) {
                IModdedInventory moddedInventory = inventory.getNullableInventory();
                if (moddedInventory == null) continue;
                moddedInventory.saveInventory(otherPlayer, inventories.get(inventory));
            }
        }
    }

    @Override
    public ItemStack slotClick(int slotId, int clickedButton, int mode, EntityPlayer player) {
        modifiedInventories.add(activeInventory);
        return super.slotClick(slotId, clickedButton, mode, player);
    }
}
