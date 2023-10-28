package serverutils.lib.item;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.oredict.OreDictionary;

import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry;
import serverutils.lib.util.InvUtils;

public class ItemStackSerializer {

    public static ItemStack parseItemThrowingException(String input) throws Exception {
        input = input.trim();
        if (input.isEmpty() || input.equals("-") || input.equals("minecraft:air")) {
            return InvUtils.EMPTY_STACK;
        } else if (input.startsWith("{")) {
            NBTTagCompound nbt = (NBTTagCompound) JsonToNBT.func_150315_a(input);

            if (nbt.getByte("Count") <= 0) {
                nbt.setByte("Count", (byte) 1);
            }

            int id = nbt.getShort("id");

            return id == 0 ? parseItemWithName(nbt) : ItemStack.loadItemStackFromNBT(nbt);
        }

        String[] s1 = input.split(" ", 4);
        Item item = GameData.getItemRegistry().getObject(new ResourceLocation(s1[0]).toString());

        if (item == null) {
            throw new NullPointerException("Unknown item: " + s1[0]);
        }

        int stackSize = 1, meta = 0;

        if (s1.length >= 2) {
            stackSize = MathHelper.parseIntWithDefault(s1[1], 1);
        }

        if (s1.length >= 3) {
            meta = (s1[2].charAt(0) == '*') ? OreDictionary.WILDCARD_VALUE : MathHelper.parseIntWithDefault(s1[2], 0);
        }

        ItemStack itemstack = new ItemStack(item, stackSize, meta);

        if (s1.length >= 4) {
            itemstack.setTagCompound((NBTTagCompound) JsonToNBT.func_150315_a(s1[3]));
        }

        return itemstack == null ? InvUtils.EMPTY_STACK : itemstack;
    }

    public static ItemStack parseItem(String input) {
        try {
            return parseItemThrowingException(input);
        } catch (Exception ex) {
            return InvUtils.EMPTY_STACK;
        }
    }

    public static ItemStack parseItemWithName(NBTTagCompound nbt) {
        String id = nbt.getString("id");
        int dmg = nbt.getShort("Damage");
        int count = nbt.getByte("Count");
        String tag = "";
        if (nbt.hasKey("tag")) {
            tag = nbt.getTag("tag").toString();
        }

        ItemStack stack = GameRegistry.makeItemStack(id, dmg, count, tag);

        if (stack != null) {
            stack.stackSize = count;
        }
        return stack;
    }

    public static String toString(ItemStack stack) {
        if (stack == null) {
            return "minecraft:air";
        }

        NBTTagCompound nbt = stack.writeToNBT(new NBTTagCompound());

        if (nbt.hasKey("ForgeCaps")) {
            return nbt.toString();
        }

        StringBuilder builder = new StringBuilder(
                String.valueOf(GameData.getItemRegistry().getNameForObject(stack.getItem())));

        int count = stack.stackSize;
        int meta = stack.getItemDamage();
        NBTTagCompound tag = stack.getTagCompound();

        if (count > 1 || meta != 0 || tag != null) {
            builder.append(' ');
            builder.append(count);
        }

        if (meta != 0 || tag != null) {
            builder.append(' ');
            builder.append(meta);
        }

        if (tag != null) {
            builder.append(' ');
            builder.append(tag);
        }

        return builder.toString();
    }

    public static NBTBase write(ItemStack stack, boolean forceCompound) {
        if (stack == null) {
            return forceCompound ? new NBTTagCompound() : new NBTTagString("");
        }

        NBTTagCompound nbt = stack.writeToNBT(new NBTTagCompound());

        if (!nbt.hasKey("ForgeCaps") && !nbt.hasKey("tag")) {
            if (!forceCompound) {
                return new NBTTagString(toString(stack));
            }

            NBTTagCompound nbt1 = new NBTTagCompound();
            nbt1.setString("item", toString(stack));
            return nbt1;
        }

        if (nbt.getByte("Count") == 1) {
            nbt.removeTag("Count");
        }

        if (nbt.getShort("Damage") == 0) {
            nbt.removeTag("Damage");
        }

        return nbt;
    }

    public static ItemStack read(@Nullable NBTBase nbtBase) {
        if (nbtBase == null) {
            return InvUtils.EMPTY_STACK;
        } else if (nbtBase instanceof NBTTagString) {
            return parseItem(((NBTTagString) nbtBase).func_150285_a_());
        } else if (!(nbtBase instanceof NBTTagCompound)) {
            return InvUtils.EMPTY_STACK;
        }

        NBTTagCompound nbt = (NBTTagCompound) nbtBase;

        if (nbt.hasKey("item", Constants.NBT.TAG_STRING)) {
            return parseItem(nbt.getString("item"));
        }

        if (!nbt.hasKey("Count")) {
            nbt.setByte("Count", (byte) 1);
        }

        return ItemStack.loadItemStackFromNBT(nbt);
    }
}
