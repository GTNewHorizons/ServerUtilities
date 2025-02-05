package serverutils.invsee.inventories;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.Constants;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.icon.Icon;
import serverutils.lib.icon.ItemIcon;

public class MainInventory implements IModdedInventory {

    private static final Icon CHEST_ICON = ItemIcon.getItemIcon(Blocks.chest);

    @Override
    public @Nullable IInventory loadOnlineInventory(EntityPlayerMP player) {
        return player.inventory;
    }

    @Override
    public @Nullable IInventory loadOfflineInventory(ForgePlayer player) {
        NBTTagCompound tag = player.getPlayerNBT();
        InventoryPlayer playerInv = new InventoryPlayer(null);
        playerInv.readFromNBT(tag.getTagList("Inventory", Constants.NBT.TAG_COMPOUND));
        return playerInv;
    }

    @Override
    public @NotNull IInventory createInventory(EntityPlayer player, int size) {
        return new InventoryPlayer(player);
    }

    @Override
    public @NotNull Icon getButtonIcon() {
        return CHEST_ICON;
    }

    @Override
    public @NotNull String getButtonText() {
        return "Main Inventory";
    }

    @Override
    public void saveInventory(ForgePlayer player, IInventory inventory) {
        if (player.isOnline()) return;
        NBTTagCompound tag = player.getPlayerNBT();
        InventoryPlayer invPlayer = (InventoryPlayer) inventory;
        NBTTagList invTag = new NBTTagList();
        invPlayer.writeToNBT(invTag);
        tag.setTag("Inventory", invTag);
        player.setPlayerNBT(tag);
    }

    @Override
    public @Nullable Slot getSlot(EntityPlayer player, IInventory inventory, int index, int x, int y) {
        if (index >= inventory.getSizeInventory() - 4) {
            int armorSlot = getArmorSlotIndex(index, inventory);
            return new SlotArmor(inventory, armorSlot, x, y, player, inventory.getSizeInventory() - 1 - armorSlot);
        }

        // shuffles the slots to be in the same order as the viewing players inventory
        if (index >= 9 && index < 18) {
            index += 18;
        } else if (index >= 27 && index < 36) {
            index -= 18;
        }

        return new Slot(inventory, index, x, y);
    }

    public int getArmorSlotIndex(int index, IInventory inventory) {
        // this is nasty, but it reorders the slots to be -> Helmet, Chestplate, Leggings, Boots
        // instead of -> Boots, Leggings, Chestplate, Helmet
        if (index == inventory.getSizeInventory() - 4) {
            return inventory.getSizeInventory() - 1;
        } else if (index == inventory.getSizeInventory() - 3) {
            return inventory.getSizeInventory() - 2;
        } else if (index == inventory.getSizeInventory() - 2) {
            return inventory.getSizeInventory() - 3;
        }
        return inventory.getSizeInventory() - 4;
    }

    private static class SlotArmor extends Slot {

        private final int armorSlot;
        private final EntityPlayer player;

        private SlotArmor(IInventory inventory, int index, int x, int y, EntityPlayer player, int armorSlot) {
            super(inventory, index, x, y);
            this.player = player;
            this.armorSlot = armorSlot;
        }

        @Override
        public int getSlotStackLimit() {
            return 1;
        }

        @Override
        public boolean isItemValid(@Nullable ItemStack stack) {
            if (stack == null) return false;
            return stack.getItem().isValidArmor(stack, armorSlot, player);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public IIcon getBackgroundIconIndex() {
            return ItemArmor.func_94602_b(armorSlot);
        }
    }
}
