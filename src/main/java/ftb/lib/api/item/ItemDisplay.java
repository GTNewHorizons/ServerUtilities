package ftb.lib.api.item;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.util.Constants;

import ftb.lib.LMNBTUtils;

public class ItemDisplay {

    public final ItemStack item;
    public final String title;
    public final List<String> desc;
    public final float scale;

    public ItemDisplay(ItemStack is, String t, List<String> d, float s) {
        item = (is == null) ? new ItemStack(Blocks.stone) : is;
        title = (t == null) ? "" : t;
        desc = (d == null) ? new ArrayList<String>() : d;
        scale = MathHelper.clamp_float(s, 1F, 8F);
    }

    public void writeToNBT(NBTTagCompound tag) {
        NBTTagCompound tag1 = new NBTTagCompound();
        item.writeToNBT(tag1);
        tag.setTag("I", tag1);
        tag.setString("T", title);
        tag.setTag("D", LMNBTUtils.fromStringList(desc));
        tag.setFloat("S", scale);
    }

    public static ItemDisplay readFromNBT(NBTTagCompound tag) {
        return new ItemDisplay(
                ItemStack.loadItemStackFromNBT(tag.getCompoundTag("I")),
                tag.getString("T"),
                LMNBTUtils.toStringList(tag.getTagList("D", Constants.NBT.TAG_STRING)),
                tag.getFloat("S"));
    }
}
