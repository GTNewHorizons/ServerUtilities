package ftb.lib.api.item;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraftforge.common.util.Constants;

public class StringIDInvLoader {

    public static void readItemsFromNBT(ItemStack[] items, NBTTagCompound tag, String s) {
        if (items == null || items.length == 0) return;
        Arrays.fill(items, null);

        if (tag.hasKey(s)) {
            NBTTagList list = tag.getTagList(s, Constants.NBT.TAG_COMPOUND);

            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound tag1 = list.getCompoundTagAt(i);
                Map.Entry<Integer, ItemStack> itemEntry = loadFromNBT(tag1);
                if (itemEntry != null) {
                    int key = itemEntry.getKey().intValue();
                    if (key >= 0 && key < items.length) items[key] = itemEntry.getValue();
                }
            }
        }
    }

    public static void writeItemsToNBT(ItemStack[] items, NBTTagCompound tag, String s) {
        if (items == null || items.length == 0) return;
        NBTTagList list = new NBTTagList();

        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) list.appendTag(saveToNBT(items[i], i));
        }

        if (list.tagCount() > 0) tag.setTag(s, list);
    }

    public static void readInvFromNBT(IInventory inv, NBTTagCompound tag, String s) {
        if (inv == null) return;
        ItemStack[] items = new ItemStack[inv.getSizeInventory()];
        readItemsFromNBT(items, tag, s);
        for (int i = 0; i < items.length; i++) inv.setInventorySlotContents(i, items[i]);
        inv.markDirty();
    }

    public static void writeInvToNBT(IInventory inv, NBTTagCompound tag, String s) {
        if (inv == null) return;
        ItemStack[] items = new ItemStack[inv.getSizeInventory()];
        for (int i = 0; i < items.length; i++) items[i] = inv.getStackInSlot(i);
        writeItemsToNBT(items, tag, s);
    }

    public static int getSlotsUsed(NBTTagCompound tag, String s) {
        return tag.hasKey(s) ? tag.getTagList(s, Constants.NBT.TAG_COMPOUND).tagCount() : 0;
    }

    public static int getItemCount(NBTTagCompound tag, String s) {
        int count = 0;

        if (tag.hasKey(s)) {
            NBTTagList list = tag.getTagList(s, Constants.NBT.TAG_COMPOUND);

            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound tag1 = list.getCompoundTagAt(i);

                if (tag1.hasKey("S")) count += tag1.getByte("C");
                else {
                    int[] ai = tag1.getIntArray("D");
                    if (ai.length == 3) count += ai[1];
                }
            }
        }

        return count;
    }

    public static Map.Entry<Integer, ItemStack> loadFromNBT(NBTTagCompound tag) {
        if (tag == null || tag.hasNoTags()) return null;

        if (tag.hasKey("Slot")) {
            int slot = tag.getShort("Slot");
            ItemStack is = ItemStack.loadItemStackFromNBT(tag);
            if (is != null) return new AbstractMap.SimpleEntry<>(Integer.valueOf(slot), is);
        } else {
            Item item = LMInvUtils.getItemFromRegName(tag.getString("ID"));

            if (item != null) {
                if (tag.hasKey("S")) {
                    int slot = tag.getShort("S");

                    int size = tag.getByte("C");
                    int dmg = Math.max(0, tag.getShort("D"));
                    ItemStack is = new ItemStack(item, size, dmg);
                    if (tag.hasKey("T")) is.setTagCompound(tag.getCompoundTag("T"));
                    return new AbstractMap.SimpleEntry<>(Integer.valueOf(slot), is);
                } else {
                    int[] ai = tag.getIntArray("D");

                    if (ai.length == 3) {
                        ItemStack is = new ItemStack(item, ai[1], ai[2]);
                        if (tag.hasKey("T", Constants.NBT.TAG_COMPOUND)) is.setTagCompound(tag.getCompoundTag("T"));
                        return new AbstractMap.SimpleEntry<>(Integer.valueOf(ai[0]), is);
                    }
                }
            }
        }

        return null;
    }

    public static NBTTagCompound saveToNBT(ItemStack is, int slot) {
        if (is == null || is.getItem() == null) return null;
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("ID", LMInvUtils.getRegName(is.getItem()).toString());
        tag.setIntArray("D", new int[] { slot, is.stackSize, is.getItemDamage() });
        if (is.hasTagCompound()) tag.setTag("T", is.getTagCompound());
        return tag;
    }
}
