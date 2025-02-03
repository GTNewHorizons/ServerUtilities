package serverutils.invsee.inventories;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import serverutils.lib.data.ForgePlayer;
import serverutils.lib.icon.Icon;

public interface IModdedInventory {

    @NotNull
    IInventory loadOnlineInventory(EntityPlayer player);

    @Nullable
    IInventory loadOfflineInventory(ForgePlayer player);

    @NotNull
    IInventory createInventory(EntityPlayer player);

    @NotNull
    Icon getButtonIcon();

    @NotNull
    String getButtonText();

    void saveInventory(ForgePlayer player, IInventory inventory);

    @Nullable
    default Slot getSlot(EntityPlayer player, IInventory inventory, int index, int x, int y) {
        return new Slot(inventory, index, x, y);
    }
}
