package serverutils.invsee;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import serverutils.invsee.inventories.IModdedInventory;
import serverutils.invsee.inventories.InvSeeRegistry;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.gui.ContainerBase;
import serverutils.net.MessageInvseeContainer;

public class InvseeContainer extends ContainerBase {

    private static final int SLOT_SIZE = 16;
    private static final int MARGIN = 8;
    private static final int VANILLA_WIDTH = 176;
    private static final int PLAYER_GRID_WIDTH = 8 * 18 + SLOT_SIZE;
    private static final Map<ForgePlayer, Object> offlineRevisions = new WeakHashMap<>();

    private final Map<IModdedInventory, IInventory> inventories;
    private final ForgePlayer otherPlayer;
    private final EntityPlayerMP otherPlayerEntity;
    private final Map<IModdedInventory, List<Slot>> moddedInventorySlots = new HashMap<>();
    private final Set<IModdedInventory> modifiedInventories = new HashSet<>();
    private final Map<IModdedInventory, Object> inventoryLayouts = new HashMap<>();
    private IModdedInventory activeInventory;
    private int playerSlotStart;
    private int armorSlotStart = -1;
    private Object offlineRevision;

    public InvseeContainer(Map<IModdedInventory, IInventory> moddedInventories, EntityPlayer player,
            @Nullable ForgePlayer otherPlayer) {
        super(player);
        this.inventories = moddedInventories;
        this.otherPlayer = otherPlayer;
        this.otherPlayerEntity = otherPlayer == null ? null : otherPlayer.getNullablePlayer();
        if (otherPlayer != null && otherPlayerEntity == null) {
            offlineRevision = offlineRevisions.computeIfAbsent(otherPlayer, key -> new Object());
        }

        for (Map.Entry<IModdedInventory, IInventory> entry : moddedInventories.entrySet()) {
            IInventory inventory = entry.getValue();
            inventoryLayouts.put(entry.getKey(), entry.getKey().getLayoutIdentity(inventory));

            List<Slot> inventorySlots = moddedInventorySlots
                    .computeIfAbsent(entry.getKey(), a -> new ArrayList<>(inventory.getSizeInventory()));
            // the last row is always anchored at the bottom, rows are stacked upwards from there
            IModdedInventory moddedInventory = entry.getKey();
            int columns = moddedInventory.getColumns(inventory);
            int lastRow = (inventory.getSizeInventory() - 1) / columns;
            boolean bottomUp = moddedInventory.isBottomUpLayout();
            int slotsInRow = 0;
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                if (slotsInRow == columns) slotsInRow = 0;
                int row = bottomUp ? i / columns : lastRow - i / columns;
                Slot slot = moddedInventory.getSlot(player, inventory, i, 8 + slotsInRow++ * 18, 54 - row * 18);
                if (slot != null) {
                    inventorySlots.add(slot);
                } else if (slotsInRow > 0) {
                    slotsInRow--;
                }
            }

            inventorySlots.sort(Comparator.comparingInt(Slot::getSlotIndex));
        }

        setActiveInventory(InvSeeRegistry.getMainInventory());
    }

    public void setActiveInventory(IModdedInventory inventory) {
        if (!canInteractWith(player) || !moddedInventorySlots.containsKey(inventory)) return;
        func_94533_d();
        activeInventory = inventory;
        inventorySlots.clear();
        inventoryItemStacks.clear();
        armorSlotStart = -1;
        for (Slot slot : moddedInventorySlots.get(inventory)) {
            addSlotToContainer(slot);
            if (slot.yDisplayPosition < 0 && armorSlotStart == -1) {
                armorSlotStart = slot.slotNumber;
            }
        }

        playerSlotStart = inventorySlots.size();
        // the player grid is always 9 wide, so it has to be centered under inventories that are wider than that
        addPlayerSlots((getWidth() - PLAYER_GRID_WIDTH) / 2, 85);
        detectAndSendChanges();
    }

    public void switchInventory(IModdedInventory inventory) {
        if (!(player instanceof EntityPlayerMP serverPlayer) || otherPlayer == null
                || !canInteractWith(player)
                || inventory == activeInventory
                || !moddedInventorySlots.containsKey(inventory))
            return;
        saveModifiedInventories();
        serverPlayer.getNextWindowId();
        windowId = serverPlayer.currentWindowId;
        setPlayerIsPresent(player, true);
        new MessageInvseeContainer(otherPlayer, inventories, windowId, inventory).sendTo(serverPlayer);
        setActiveInventory(inventory);
        serverPlayer.sendContainerToPlayer(this);
    }

    /**
     * Width the gui needs to fit the active inventory, never narrower than a vanilla container.
     */
    public int getWidth() {
        int rightmostSlot = inventorySlots.stream().mapToInt(e -> e.xDisplayPosition).max().orElse(0);
        return Math.max(VANILLA_WIDTH, rightmostSlot + SLOT_SIZE + MARGIN);
    }

    public boolean isArmorSlot(int containerIndex) {
        return armorSlotStart >= 0 && containerIndex >= armorSlotStart && containerIndex < playerSlotStart;
    }

    public IModdedInventory getActiveInventory() {
        return activeInventory;
    }

    public int getHighestSlot() {
        return inventorySlots.stream().mapToInt(e -> e.yDisplayPosition).min().orElse(0);
    }

    public int getLowestSlot() {
        return inventorySlots.stream().mapToInt(e -> e.yDisplayPosition).max().orElse(0);
    }

    @Override
    public int getNonPlayerSlots() {
        return playerSlotStart;
    }

    @Override
    public void detectAndSendChanges() {
        if (!canInteractWith(player)) {
            if (!player.worldObj.isRemote && player.openContainer == this) player.closeScreen();
            return;
        }
        super.detectAndSendChanges();

        if (!player.worldObj.isRemote && otherPlayer != null && otherPlayer.isOnline()) {
            Container container = otherPlayer.getPlayer().openContainer;
            if (!(container instanceof InvseeContainer) && !container.crafters.contains((EntityPlayerMP) player)) {
                container.detectAndSendChanges();
            }
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        if (player != this.player) return false;
        if (otherPlayer != null && otherPlayer.getNullablePlayer() != otherPlayerEntity) return false;
        if (offlineRevision != null && offlineRevisions.get(otherPlayer) != offlineRevision) return false;
        for (Map.Entry<IModdedInventory, IInventory> entry : inventories.entrySet()) {
            if (!Objects
                    .equals(inventoryLayouts.get(entry.getKey()), entry.getKey().getLayoutIdentity(entry.getValue()))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);

        if (!player.worldObj.isRemote && otherPlayer != null) {
            saveModifiedInventories();
            if (!canInteractWith(player) && player instanceof EntityPlayerMP serverPlayer) {
                serverPlayer.sendContainerToPlayer(serverPlayer.inventoryContainer);
            }
        }
    }

    @Override
    public ItemStack slotClick(int slotId, int clickedButton, int mode, EntityPlayer player) {
        if (!canInteractWith(player) || slotId >= inventorySlots.size()
                || slotId < -1 && slotId != -999
                || mode < 0
                || mode > 6
                || clickedButton < 0)
            return null;
        if (mode == 2 ? clickedButton > 8
                : mode == 3 ? clickedButton != 2
                        : mode == 5 ? clickedButton > 10 || (clickedButton & 3) > 2 : clickedButton > 1)
            return null;
        modifiedInventories.add(activeInventory);
        try {
            return super.slotClick(slotId, clickedButton, mode, player);
        } finally {
            if (otherPlayer != null && !otherPlayer.isOnline()) saveModifiedInventories();
        }
    }

    private void saveModifiedInventories() {
        if (player.worldObj.isRemote || otherPlayer == null
                || otherPlayer.getNullablePlayer() != otherPlayerEntity
                || offlineRevision != null && offlineRevisions.get(otherPlayer) != offlineRevision)
            return;
        List<IModdedInventory> modified = new ArrayList<>(modifiedInventories);
        modified.sort(Comparator.comparingInt(InvSeeRegistry.getRegisteredInventories()::indexOf));
        for (IModdedInventory inventory : modified) {
            inventory.saveInventory(otherPlayer, inventories.get(inventory));
        }
        modifiedInventories.clear();
        if (!modified.isEmpty() && offlineRevision != null) {
            offlineRevision = new Object();
            offlineRevisions.put(otherPlayer, offlineRevision);
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (!canInteractWith(player) || index < 0 || index >= inventorySlots.size()) return null;
        Slot slot = inventorySlots.get(index);
        if (!slot.canTakeStack(player)) return null;
        modifiedInventories.add(activeInventory);
        return super.transferStackInSlot(player, index);
    }

    @Override
    protected boolean mergeItemStack(ItemStack source, int start, int end, boolean reverse) {
        boolean changed = false;
        for (int pass = 0; pass < 2 && source.stackSize > 0; pass++) {
            for (int index = reverse ? end - 1 : start; source.stackSize > 0 && index >= start
                    && index < end; index += reverse ? -1 : 1) {
                Slot slot = inventorySlots.get(index);
                ItemStack target = slot.getStack();
                if ((pass == 0) != (target != null) || !slot.isItemValid(source)) continue;
                int limit = Math.min(source.getMaxStackSize(), slot.getSlotStackLimit());
                if (target != null) {
                    if (!source.isStackable() || target.getItem() != source.getItem()
                            || source.getHasSubtypes() && target.getItemDamage() != source.getItemDamage()
                            || !ItemStack.areItemStackTagsEqual(target, source))
                        continue;
                    int amount = Math.min(source.stackSize, Math.max(0, limit - target.stackSize));
                    if (amount == 0) continue;
                    target.stackSize += amount;
                    source.stackSize -= amount;
                    slot.onSlotChanged();
                } else {
                    int amount = Math.min(source.stackSize, limit);
                    if (amount <= 0) continue;
                    slot.putStack(source.splitStack(amount));
                }
                changed = true;
            }
        }
        return changed;
    }
}
