package serverutils.invsee.inventories;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import de.eydamos.backpack.inventory.InventoryBackpack;
import de.eydamos.backpack.inventory.slot.SlotBackpack;
import de.eydamos.backpack.item.ItemsBackpack;
import de.eydamos.backpack.saves.BackpackSave;
import de.eydamos.backpack.saves.PlayerSave;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.icon.Icon;
import serverutils.lib.icon.ItemIcon;

public class MinecraftBackpackInv implements IModdedInventory {

    private static final Icon BACKPACK_ICON = ItemIcon.getItemIcon(ItemsBackpack.backpack);

    @Override
    public @Nullable IInventory loadOnlineInventory(EntityPlayerMP player) {
        return loadInventory(player.getUniqueID().toString());
    }

    @Override
    public @Nullable IInventory loadOfflineInventory(ForgePlayer player) {
        return loadInventory(player.getId().toString());
    }

    @Override
    public @NotNull IInventory createInventory(EntityPlayer player, int size) {
        return new InventoryBasic("", false, size);
    }

    @Override
    public @NotNull Icon getButtonIcon() {
        return BACKPACK_ICON;
    }

    @Override
    public @NotNull String getButtonText() {
        return "Personal Backpack";
    }

    @Override
    public @Nullable Slot getSlot(EntityPlayer player, IInventory inventory, int index, int x, int y) {
        return new SlotBackpack(inventory, index, x, y);
    }

    @Override
    public void saveInventory(ForgePlayer player, IInventory inventory) {
        if (!(inventory instanceof InventoryBackpack backpack)) return;
        backpack.markDirty();
        PlayerSave playerSave = new PlayerSave(player.getId().toString());
        BackpackSave backpackSave = new BackpackSave(playerSave.getPersonalBackpack());
        backpack.writeToNBT(backpackSave);
        backpackSave.save();
    }

    public static InventoryBackpack loadInventory(String uuid) {
        PlayerSave playerSave = new PlayerSave(uuid);
        if (!playerSave.hasPersonalBackpack()) return null;
        InventoryBackpack backpack = new InventoryBackpack("", "");
        backpack.readFromNBT(new BackpackSave(playerSave.getPersonalBackpack()));
        return backpack;
    }
}
