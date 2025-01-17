package serverutils.command;

import java.lang.reflect.Method;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.FMLLog;
import serverutils.lib.util.InvUtils;

public class InvSeeInventory implements IInventory {

    public static final int[] slotMapping = { 39, 38, 37, 36, -1, 40, 41, 42, 43, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
            19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 0, 1, 2, 3, 4, 5, 6, 7, 8, };
    private final IInventory inventory;
    private final EntityPlayerMP player;
    private final IInventory baubles;
    private static Method getBaubles = null;
    public boolean hasChanged;
    private Consumer<InvSeeInventory> saveCallback;

    public InvSeeInventory(IInventory inv, @Nullable EntityPlayerMP ep) {
        inventory = inv;
        player = ep;
        baubles = getBaubles(ep);
    }

    @Override
    public int getSizeInventory() {
        return 45;
    }

    public int getSlot(int slot) {
        return (slot == -1) ? -1 : (slot % 40);
    }

    public IInventory getInv(int slot) {
        if (slot == -1) return null;
        if (slot >= 40) return baubles;
        return inventory;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        int slot = slotMapping[index];
        IInventory inv = getInv(slot);
        return (inv == null) ? null : inv.getStackInSlot(getSlot(slot));
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        int slot = slotMapping[index];
        IInventory inv = getInv(slot);
        return (inv == null) ? null : inv.decrStackSize(getSlot(slot), count);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack is) {
        int slot = slotMapping[index];
        IInventory inv = getInv(slot);
        if (inv != null) {
            inv.setInventorySlotContents(getSlot(slot), is);
            inv.markDirty();
        }
    }

    @Override
    public String getInventoryName() {
        if (player != null) {
            return player.getDisplayName();
        }

        return inventory.getInventoryName();
    }

    @Override
    public boolean hasCustomInventoryName() {
        return true;
    }

    @Override
    public void closeInventory() {
        if (hasChanged && saveCallback != null) {
            saveCallback.accept(this);
        }
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int index) {
        int slot = slotMapping[index];
        IInventory inv = getInv(slot);
        return inv == null ? null : inv.getStackInSlotOnClosing(getSlot(slot));
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory() {}

    @Override
    public int getInventoryStackLimit() {
        return inventory.getInventoryStackLimit();
    }

    @Override
    public void markDirty() {
        hasChanged = true;
        inventory.markDirty();

        if (player != null) {
            player.openContainer.detectAndSendChanges();
        }
        if (baubles != null) {
            baubles.markDirty();
        }
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        int slot = slotMapping[index];
        IInventory inv = getInv(slot);
        return inv != null && inv.isItemValidForSlot(getSlot(slot), stack);
    }

    public void clear() {
        InvUtils.clear(inventory);
        InvUtils.clear(baubles);
    }

    public void setSaveCallback(Consumer<InvSeeInventory> callback) {
        saveCallback = callback;
    }

    public InventoryPlayer getPlayerInv() {
        return (InventoryPlayer) inventory;
    }

    public static IInventory getBaubles(EntityPlayer player) {
        IInventory ot = null;

        try {
            if (getBaubles == null) {
                Class<?> fake = Class.forName("baubles.common.lib.PlayerHandler");
                getBaubles = fake.getMethod("getPlayerBaubles", EntityPlayer.class);
            }

            ot = (IInventory) getBaubles.invoke(null, player);
        } catch (Exception ex) {
            FMLLog.warning("[Baubles API] Could not invoke baubles.common.lib.PlayerHandler method getPlayerBaubles");
        }

        return ot;
    }
}
