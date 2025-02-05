package serverutils.invsee.inventories;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.darkona.adventurebackpack.inventory.IInventoryTanks;
import com.darkona.adventurebackpack.inventory.InventoryBackpack;
import com.darkona.adventurebackpack.item.ItemAdventureBackpack;
import com.darkona.adventurebackpack.reference.BackpackTypes;
import com.darkona.adventurebackpack.util.BackpackUtils;
import com.darkona.adventurebackpack.util.Wearing;

import serverutils.lib.data.ForgePlayer;
import serverutils.lib.icon.Icon;
import serverutils.lib.icon.ItemIcon;

public class AdventureBackpackInv implements IModdedInventory {

    private static final Icon BACKPACK_ICON = ItemIcon
            .getItemIcon(BackpackUtils.createBackpackStack(BackpackTypes.RAINBOW));
    private static final String WEARABLE_TAG = "wearable";

    @Override
    public @Nullable IInventory loadOnlineInventory(EntityPlayerMP player) {
        if (!Wearing.isWearingBackpack(player)) return null;
        return Wearing.getWearingBackpackInv(player);
    }

    @Override
    public @Nullable IInventory loadOfflineInventory(ForgePlayer player) {
        NBTTagCompound backpackTag = player.getPlayerNBT();
        if (!backpackTag.hasKey(WEARABLE_TAG)) return null;
        ItemStack wearable = ItemStack.loadItemStackFromNBT(backpackTag.getCompoundTag(WEARABLE_TAG));
        if (wearable == null || !(wearable.getItem() instanceof ItemAdventureBackpack)) return null;
        return new InventoryBackpack(wearable);
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
        return "Adventure Backpack";
    }

    @Override
    public void saveInventory(ForgePlayer player, IInventory inventory) {
        if (player.isOnline()) return;
        NBTTagCompound tag = player.getPlayerNBT();
        inventory.closeInventory();
        tag.setTag(WEARABLE_TAG, ((IInventoryTanks) inventory).getParentItem().writeToNBT(new NBTTagCompound()));
        player.setPlayerNBT(tag);
    }
}
