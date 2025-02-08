package serverutils.invsee.inventories;

import static micdoodle8.mods.galacticraft.core.entities.player.GCPlayerStats.GC_PLAYER_PROP;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import micdoodle8.mods.galacticraft.core.GalacticraftCore;
import micdoodle8.mods.galacticraft.core.entities.player.GCPlayerStats;
import micdoodle8.mods.galacticraft.core.inventory.InventoryExtended;
import micdoodle8.mods.galacticraft.core.inventory.SlotExtendedInventory;
import micdoodle8.mods.galacticraft.core.items.GCItems;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.icon.Icon;
import serverutils.lib.icon.ImageIcon;
import serverutils.lib.icon.ItemIcon;
import serverutils.lib.icon.PartIcon;

public class GalacticraftInventory implements IModdedInventory {

    private static final Icon[] SLOT_OVERLAYS = new Icon[10];
    private static final Icon GC_BUTTON_ICON = ItemIcon.getItemIcon(GCItems.oxMask);
    private static final String GC_EXTENDED_INV = "ExtendedInventoryGC";

    static {
        ImageIcon icon = new ImageIcon(
                new ResourceLocation(GalacticraftCore.ASSET_PREFIX, "textures/gui/inventory.png"));
        for (int i = 0; i < 10; i++) {
            SLOT_OVERLAYS[i] = switch (i) {
                case 0 -> new PartIcon(icon, 125, 17, 16, 16, 4);
                case 1 -> new PartIcon(icon, 125, 35, 16, 16, 4);
                case 2, 3 -> new PartIcon(icon, 116, 53, 16, 16, 4);
                case 4 -> new PartIcon(icon, 143, 17, 16, 16, 4);
                case 5 -> new PartIcon(icon, 107, 17, 16, 16, 4);
                case 6, 7, 8, 9 -> new PartIcon(icon, 79, 8 + ((i - 6) * 18), 16, 16, 4);
                default -> ItemIcon.EMPTY;
            };
        }
    }

    @Override
    public @Nullable IInventory loadOnlineInventory(EntityPlayerMP player) {
        return GCPlayerStats.get(player).extendedInventory;
    }

    @Override
    public @Nullable IInventory loadOfflineInventory(ForgePlayer player) {
        InventoryExtended inventory = new InventoryExtended();
        NBTTagCompound gcStats = player.getPlayerNBT().getCompoundTag(GC_PLAYER_PROP);
        inventory.readFromNBT(gcStats.getTagList(GC_EXTENDED_INV, Constants.NBT.TAG_COMPOUND));
        return inventory;
    }

    @Override
    public @NotNull IInventory createInventory(EntityPlayer player, int size) {
        return new InventoryExtended();
    }

    @Override
    public @NotNull Icon getButtonIcon() {
        return GC_BUTTON_ICON;
    }

    @Override
    public @NotNull String getButtonText() {
        return "Galacticraft";
    }

    @Override
    public String getInventoryName() {
        return "GC Items";
    }

    @Override
    public @Nullable Slot getSlot(EntityPlayer player, IInventory inventory, int index, int x, int y) {
        return switch (index) {
            case 0 -> new SlotExtendedInventory(inventory, index, 100, 17);
            case 1 -> new SlotExtendedInventory(inventory, index, 100, 35);
            case 2 -> new SlotExtendedInventory(inventory, index, 91, 53);
            case 3 -> new SlotExtendedInventory(inventory, index, 109, 53);
            case 4 -> new SlotExtendedInventory(inventory, index, 118, 17);
            case 5 -> new SlotExtendedInventory(inventory, index, 82, 17);
            case 6, 7, 8, 9 -> new SlotExtendedInventory(inventory, index, 54, 8 + ((index - 6) * 18));
            default -> null;
        };
    }

    @Override
    public @Nullable Icon getSlotOverlay(Slot slot) {
        if (slot.getSlotIndex() >= 10) return null;
        return SLOT_OVERLAYS[slot.getSlotIndex()];
    }

    @Override
    public void saveInventory(ForgePlayer player, IInventory inventory) {
        if (player.isOnline()) return;
        NBTTagCompound tag = player.getPlayerNBT();
        NBTTagCompound gcStats = tag.getCompoundTag(GC_PLAYER_PROP);
        NBTTagList invList = new NBTTagList();
        ((InventoryExtended) inventory).writeToNBT(invList);
        gcStats.setTag(GC_EXTENDED_INV, invList);
        tag.setTag(GC_PLAYER_PROP, gcStats);
        player.setPlayerNBT(tag);
    }
}
