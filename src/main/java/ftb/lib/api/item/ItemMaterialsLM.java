package ftb.lib.api.item;

import java.util.HashMap;
import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public abstract class ItemMaterialsLM extends ItemLM {

    public final HashMap<Integer, MaterialItem> materials;
    public String folder = "";
    private boolean requiresMultipleRenderPasses = false;

    public ItemMaterialsLM(String s) {
        super(s);
        materials = new HashMap<>();
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    public ItemStack add(MaterialItem m) {
        materials.put(m.damage, m);

        if (m.getRenderPasses() > 1) requiresMultipleRenderPasses = true;

        return m.getStack();
    }

    public String getUnlocalizedName(ItemStack is) {
        MaterialItem m = materials.get(is.getItemDamage());
        if (m != null) return m.getUnlocalizedName();
        return "unknown";
    }

    public void onPostLoaded() {
        for (MaterialItem m : materials.values()) m.onPostLoaded();
    }

    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs c, List l) {
        for (MaterialItem m : materials.values()) l.add(new ItemStack(item, 1, m.damage));
    }

    public void loadRecipes() {
        for (MaterialItem m : materials.values()) m.loadRecipes();
    }

    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack is, EntityPlayer ep, List l, boolean b) {
        MaterialItem m = materials.get(is.getItemDamage());
        if (m != null) m.addInfo(ep, l);
    }

    public int getRenderPasses(int i) {
        if (!requiresMultipleRenderPasses) return 1;
        MaterialItem m = materials.get(i);
        if (m != null) return m.getRenderPasses();
        return 1;
    }

    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister ir) {
        for (MaterialItem m : materials.values()) m.registerIcons(ir);
    }

    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int dmg) {
        MaterialItem m = materials.get(dmg);
        if (m != null) return m.getIcon();
        return Items.egg.getIconFromDamage(0);
    }
}
