package serverutils.invsee.inventories;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryEnderChest;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import serverutils.lib.data.ForgePlayer;
import serverutils.lib.icon.Icon;
import serverutils.lib.icon.ItemIcon;

public class EnderInventory implements IModdedInventory {

    private static final Icon ENDER_CHEST_ICON = ItemIcon.getItemIcon(Blocks.ender_chest);

    @Override
    public @Nullable IInventory loadOnlineInventory(EntityPlayerMP player) {
        return player.getInventoryEnderChest();
    }

    @Override
    public @Nullable IInventory loadOfflineInventory(ForgePlayer player) {
        InventoryEnderChest enderInv = new InventoryEnderChest();
        enderInv.loadInventoryFromNBT(player.getPlayerNBT().getTagList("EnderItems", Constants.NBT.TAG_COMPOUND));
        return enderInv;
    }

    @Override
    public @NotNull IInventory createInventory(EntityPlayer player, int size) {
        return new InventoryEnderChest();
    }

    @Override
    public @NotNull Icon getButtonIcon() {
        return ENDER_CHEST_ICON;
    }

    @Override
    public @NotNull String getButtonText() {
        return "Ender Chest";
    }

    @Override
    public void saveInventory(ForgePlayer player, IInventory inventory) {
        if (player.isOnline()) return;
        NBTTagCompound tag = player.getPlayerNBT();
        InventoryEnderChest enderInv = (InventoryEnderChest) inventory;
        tag.setTag("EnderItems", enderInv.saveInventoryToNBT());
        player.setPlayerNBT(tag);
    }
}
