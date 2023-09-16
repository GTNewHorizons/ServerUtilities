package ftb.lib.api.item;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ftb.lib.LMMod;

public abstract class ItemLM extends Item implements IItemLM {

    public final String itemName;

    public ItemLM(String s) {
        super();
        itemName = s;
        setUnlocalizedName(getMod().getItemName(s));
    }

    public abstract LMMod getMod();

    @SideOnly(Side.CLIENT)
    public abstract CreativeTabs getCreativeTab();

    public final Item getItem() {
        return this;
    }

    public final String getItemID() {
        return itemName;
    }

    public void onPostLoaded() {}

    public void loadRecipes() {}

    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs c, List l) {
        l.add(new ItemStack(item, 1, 0));
    }

    @SideOnly(Side.CLIENT)
    protected String getIconString() {
        return getMod().lowerCaseModID + ":" + itemName;
    }

    public String getUnlocalizedName(ItemStack is) {
        return getMod().getItemName(itemName);
    }

    public int getRenderPasses(int m) {
        return 1;
    }

    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack is, EntityPlayer ep, List l, boolean b) {}

    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister ir) {
        itemIcon = ir.registerIcon(getIconString());
    }
}
