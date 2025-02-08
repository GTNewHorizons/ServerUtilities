package serverutils.invsee.inventories;

import java.io.File;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.expanded.BaubleExpandedSlots;
import baubles.common.container.InventoryBaubles;
import baubles.common.container.SlotBauble;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.icon.Icon;
import serverutils.lib.icon.ImageIcon;
import serverutils.lib.icon.PartIcon;
import serverutils.lib.util.CommonUtils;
import serverutils.lib.util.NBTUtils;

public class BaublesInventory implements IModdedInventory {

    private static final boolean isExpanded = CommonUtils.getClassExists("baubles.api.expanded.BaubleExpandedSlots");
    private static final Icon BAUBLES_ICON;

    static {
        ImageIcon icon;
        if (isExpanded) {
            icon = new ImageIcon(new ResourceLocation("baubles", "textures/gui/bauble_inventory.png"));
        } else {
            icon = new ImageIcon(new ResourceLocation("baubles", "textures/gui/expanded_inventory.png"));
        }

        BAUBLES_ICON = new PartIcon(icon, 210, 48, 10, 10, 4) {

            @Override
            public void draw(int x, int y, int w, int h) {
                // this icon does not look good when stretched to 16x16
                super.draw(x + 3, y + 3, 10, 10);
            }
        };
    }

    @Override
    public @Nullable IInventory loadOnlineInventory(EntityPlayerMP player) {
        return BaublesApi.getBaubles(player);
    }

    @Override
    public @Nullable IInventory loadOfflineInventory(ForgePlayer player) {
        File baublesFile = getBaublesFile(player);
        if (baublesFile == null) return null;

        NBTTagCompound data = NBTUtils.readNBT(baublesFile);
        if (data == null || data.hasNoTags()) return null;

        InventoryBaubles baubles = new InventoryBaubles(null) {

            @Override
            public void syncSlotToClients(int slot) {}
        };
        baubles.blockEvents = true;
        baubles.readNBT(data);
        return baubles;
    }

    @Override
    public @NotNull IInventory createInventory(EntityPlayer player, int size) {
        return new InventoryBaubles(player);
    }

    @Override
    public @NotNull Icon getButtonIcon() {
        return BAUBLES_ICON;
    }

    @Override
    public @NotNull String getButtonText() {
        return "Baubles";
    }

    @Override
    public void saveInventory(ForgePlayer player, IInventory inventory) {
        if (!player.isOnline()) {
            InventoryBaubles baubles = (InventoryBaubles) inventory;
            NBTTagCompound data = new NBTTagCompound();
            baubles.saveNBT(data);
            File baublesFile = getBaublesFile(player);
            if (baublesFile != null) {
                NBTUtils.writeNBTSafe(baublesFile, data);
            }
        }
    }

    @Override
    public @Nullable Slot getSlot(EntityPlayer player, IInventory inventory, int index, int x, int y) {
        if (isExpanded) {
            String slotType = BaubleExpandedSlots.getSlotType(index);
            if (slotType.equals(BaubleExpandedSlots.unknownType)) return null;
            return new SlotBauble(inventory, BaubleExpandedSlots.getSlotType(index), index, x, y);
        }

        BaubleType type;
        if (index == 0) {
            type = BaubleType.AMULET;
        } else if (index == 1 || index == 2) {
            type = BaubleType.RING;
        } else {
            type = BaubleType.BELT;
        }
        // noinspection deprecation
        return new SlotBauble(inventory, type, index, x, y);
    }

    private @Nullable File getBaublesFile(ForgePlayer player) {
        File baublesFile = new File(
                player.team.universe.getWorldDirectory(),
                "playerdata/" + player.getName() + ".baub");
        if (!baublesFile.exists()) {
            baublesFile = new File(
                    player.team.universe.getWorldDirectory(),
                    "playerdata/" + player.getName() + ".baubback");
        }
        if (!baublesFile.exists()) return null;

        return baublesFile;
    }
}
