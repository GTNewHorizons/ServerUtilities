package serverutils.invsee.inventories;

import static mods.battlegear2.api.core.IInventoryPlayerBattle.OFFSET;
import static mods.battlegear2.api.core.IInventoryPlayerBattle.WEAPON_SETS;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.battlegear2.Battlegear;
import mods.battlegear2.api.core.BattlegearUtils;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.icon.Icon;
import serverutils.lib.icon.ImageIcon;

public class BattlegearInventory implements IModdedInventory {

    private static final int SLOT_AMOUNT = WEAPON_SETS * 2;
    private static final Icon BG_ICON = new ImageIcon(
            new ResourceLocation(Battlegear.MODID, "textures/items/bg-icon.png"));

    @Override
    public @Nullable IInventory loadOnlineInventory(EntityPlayerMP player) {
        return player.inventory;
    }

    @Override
    public @Nullable IInventory loadOfflineInventory(ForgePlayer player) {
        return InvSeeInventories.MAIN.getInventory().loadOfflineInventory(player);
    }

    @Override
    public @NotNull IInventory createInventory(EntityPlayer player, int size) {
        return new InventoryPlayer(player);
    }

    @Override
    public @NotNull Icon getButtonIcon() {
        return BG_ICON;
    }

    @Override
    public @NotNull String getButtonText() {
        return "Battlegear";
    }

    @Override
    public @Nullable Slot getSlot(EntityPlayer player, IInventory inventory, int index, int x, int y) {
        if (index >= SLOT_AMOUNT) return null;

        int inventorySlot = OFFSET + index / 2;
        x = 62;
        if ((index & 1) != 0) {
            inventorySlot += WEAPON_SETS;
            x += 36;
        }
        return new WeaponSlotNoPartner(inventory, inventorySlot, x, y - 36 + (index / 2) * 18);
    }

    @Override
    public void saveInventory(ForgePlayer player, IInventory inventory) {
        if (player.isOnline()) return;
        // this is a little weird, but it can overwrite main inventory changes if we don't do this
        NBTTagCompound tag = player.getPlayerNBT();
        InventoryPlayer playerInv = new InventoryPlayer(null);
        playerInv.readFromNBT(tag.getTagList("Inventory", Constants.NBT.TAG_COMPOUND));
        for (int i = 0; i < SLOT_AMOUNT; i++) {
            int slot = OFFSET + i;
            ItemStack stack = inventory.getStackInSlot(slot);
            playerInv.setInventorySlotContents(slot, stack);
        }

        NBTTagList invTag = new NBTTagList();
        playerInv.writeToNBT(invTag);
        tag.setTag("Inventory", invTag);
        player.setPlayerNBT(tag);
    }

    private static class WeaponSlotNoPartner extends Slot {

        private final boolean mainHand;

        public WeaponSlotNoPartner(IInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
            mainHand = index < OFFSET + WEAPON_SETS;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public IIcon getBackgroundIconIndex() {
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            return Battlegear.proxy.getSlotIcon(mainHand ? 0 : 1);
        }

        @SuppressWarnings("deprecation")
        @Override
        public boolean isItemValid(@Nullable ItemStack stack) {
            if (stack == null) return true;
            if (mainHand) {
                ItemStack offHandStack = inventory.getStackInSlot(getSlotIndex() + WEAPON_SETS);
                return BattlegearUtils.isMainHand(stack, offHandStack);
            } else if (BattlegearUtils.isOffHand(stack)) {
                ItemStack mainHandStack = inventory.getStackInSlot(getSlotIndex() - WEAPON_SETS);
                return BattlegearUtils.isMainHand(mainHandStack, stack);
            }
            return false;
        }
    }
}
