package ftb.lib.api.item;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import latmod.lib.util.FinalIDObject;

public class MaterialItem extends FinalIDObject {

    public final ItemMaterialsLM item;
    public final int damage;

    @SideOnly(Side.CLIENT)
    public IIcon icon;

    public MaterialItem(ItemMaterialsLM i, int d, String s) {
        super(s);
        item = i;
        damage = d;
    }

    public ItemStack getStack(int s) {
        return new ItemStack(item, s, damage);
    }

    public ItemStack getStack() {
        return getStack(1);
    }

    public void onPostLoaded() {}

    public void loadRecipes() {}

    public int getRenderPasses() {
        return 1;
    }

    @SideOnly(Side.CLIENT)
    public void addInfo(EntityPlayer ep, List<String> l) {}

    public String getUnlocalizedName() {
        return item.getMod().getItemName(item.folder.isEmpty() ? getID() : (item.folder + "." + getID()));
    }

    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister ir) {
        if (item.folder.isEmpty()) icon = ir.registerIcon(item.getMod().lowerCaseModID + ":" + getID());
        else icon = ir.registerIcon(item.getMod().lowerCaseModID + ":" + item.folder + "/" + getID());
    }

    @SideOnly(Side.CLIENT)
    public IIcon getIcon() {
        return icon;
    }
}
