package serverutils.invsee.inventories;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import serverutils.invsee.util.CombinedInventory;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.icon.Icon;
import serverutils.lib.icon.ImageIcon;
import serverutils.lib.icon.ItemIcon;
import serverutils.lib.icon.PartIcon;
import tconstruct.armor.inventory.SlotAccessory;
import tconstruct.armor.player.ArmorExtended;
import tconstruct.armor.player.KnapsackInventory;
import tconstruct.armor.player.TPlayerStats;
import tconstruct.library.accessory.IAccessory;

public class TiCInventory implements IModdedInventory {

    private static final Icon[] SLOT_OVERLAYS = new Icon[7];
    private static final int KNAPSACK_SIZE = new KnapsackInventory().getSizeInventory();

    static {
        ImageIcon icon = new ImageIcon(new ResourceLocation("tinker", "textures/gui/armorextended.png"));

        SLOT_OVERLAYS[0] = new PartIcon(icon, 177, 10, 16, 16, 4);
        SLOT_OVERLAYS[1] = new PartIcon(icon, 177, 28, 16, 16, 4);
        SLOT_OVERLAYS[2] = new PartIcon(icon, 213, 10, 16, 16, 4);
        SLOT_OVERLAYS[3] = new PartIcon(icon, 213, 28, 16, 16, 4);
        SLOT_OVERLAYS[4] = new PartIcon(icon, 231, 1, 16, 16, 4);
        SLOT_OVERLAYS[5] = new PartIcon(icon, 231, 19, 16, 16, 4);
        SLOT_OVERLAYS[6] = new PartIcon(icon, 231, 37, 16, 16, 4);
    }

    @Override
    public @Nullable IInventory loadOnlineInventory(EntityPlayerMP player) {
        CombinedInventory combined = new CombinedInventory();
        TPlayerStats stats = TPlayerStats.get(player);
        combined.addInventories(stats.knapsack, stats.armor);
        return combined;
    }

    @Override
    public @Nullable IInventory loadOfflineInventory(ForgePlayer player) {
        CombinedInventory combined = new CombinedInventory();
        KnapsackInventory knapsack = new KnapsackInventory();
        ArmorExtendedOffline armor = new ArmorExtendedOffline();
        NBTTagCompound tinkerTag = player.getPlayerNBT().getCompoundTag(TPlayerStats.PROP_NAME);
        armor.readFromNBT(tinkerTag);
        knapsack.readFromNBT(tinkerTag);
        combined.addInventories(knapsack, armor);
        return combined;
    }

    @Override
    public @NotNull IInventory createInventory(EntityPlayer player, int size) {
        CombinedInventory combined = new CombinedInventory();
        KnapsackInventory knapsack = new KnapsackInventory();
        ArmorExtended armor = new ArmorExtended();
        knapsack.init(player);
        armor.init(player);
        combined.addInventories(knapsack, armor);
        return combined;
    }

    @Override
    public @NotNull Icon getButtonIcon() {
        return ItemIcon.getItemIcon(Items.diamond_chestplate);
    }

    @Override
    public @NotNull String getButtonText() {
        return "TiC Bag & Accessories";
    }

    @Override
    public void saveInventory(ForgePlayer player, IInventory inventory) {
        if (player.isOnline()) return;
        CombinedInventory combined = (CombinedInventory) inventory;
        NBTTagCompound tag = player.getPlayerNBT();
        NBTTagCompound tinkerTag = tag.getCompoundTag(TPlayerStats.PROP_NAME);
        KnapsackInventory knapsack = combined.getInventory(KnapsackInventory.class);
        if (knapsack != null) knapsack.saveToNBT(tinkerTag);
        ArmorExtended armor = combined.getInventory(ArmorExtended.class);
        if (armor != null) armor.saveToNBT(tinkerTag);
        tag.setTag(TPlayerStats.PROP_NAME, tinkerTag);
        player.setPlayerNBT(tag);
    }

    @Override
    public @Nullable Slot getSlot(EntityPlayer player, IInventory inventory, int index, int x, int y) {
        if (index < KNAPSACK_SIZE) {
            return new Slot(inventory, index, x, y);
        }
        return new SlotAccessory(inventory, index, x, y, index < 4 ? 1 : 10) {

            @Override
            public boolean isItemValid(@Nullable ItemStack stack) {
                Item item = stack == null ? null : stack.getItem();
                return item instanceof IAccessory accessory
                        && accessory.canEquipAccessory(stack, getSlotIndex() - KNAPSACK_SIZE);
            }
        };
    }

    @Override
    public @Nullable Icon getSlotOverlay(Slot slot) {
        if (!(slot instanceof SlotAccessory)) return null;
        int index = slot.getSlotIndex() - KNAPSACK_SIZE;
        if (index < 0 || index >= SLOT_OVERLAYS.length) return null;
        return SLOT_OVERLAYS[index];
    }

    public static class ArmorExtendedOffline extends ArmorExtended {

        @Override
        public void markDirty() {}

        @Override
        public ItemStack decrStackSize(int slot, int quantity) {
            if (inventory[slot] == null) return null;
            if (inventory[slot].stackSize <= quantity) {
                ItemStack stack = inventory[slot];
                inventory[slot] = null;
                return stack;
            }
            ItemStack split = inventory[slot].splitStack(quantity);
            if (inventory[slot].stackSize == 0) {
                inventory[slot] = null;
            }
            return split;
        }

        @Override
        public void setInventorySlotContents(int slot, @Nullable ItemStack itemstack) {
            inventory[slot] = itemstack;
            if (itemstack != null && itemstack.stackSize > getInventoryStackLimit()) {
                itemstack.stackSize = getInventoryStackLimit();
            }
        }
    }
}
