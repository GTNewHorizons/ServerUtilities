package serverutils.invsee.inventories;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import serverutils.lib.data.ForgePlayer;
import serverutils.lib.icon.Icon;

public interface IModdedInventory {

    @Nullable
    IInventory loadOnlineInventory(EntityPlayerMP player);

    @Nullable
    IInventory loadOfflineInventory(ForgePlayer player);

    @NotNull
    IInventory createInventory(EntityPlayer player, int size);

    @NotNull
    Icon getButtonIcon();

    @NotNull
    String getButtonText();

    void saveInventory(ForgePlayer player, IInventory inventory);

    @Nullable
    default Slot getSlot(EntityPlayer player, IInventory inventory, int index, int x, int y) {
        return new Slot(inventory, index, x, y);
    }

    default @Nullable Icon getSlotOverlay(Slot slot) {
        return null;
    }

    /**
     * How many slots the inventory lays out per row in its own gui.
     */
    default int getColumns(IInventory inventory) {
        return 9;
    }

    /**
     * Whether slot index 0 belongs to the bottom row instead of the top one, like the player hotbar does.
     */
    default boolean isBottomUpLayout() {
        return false;
    }

    default String getInventoryName() {
        return getButtonText();
    }
}
