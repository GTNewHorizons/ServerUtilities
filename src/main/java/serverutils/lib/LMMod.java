package serverutils.lib;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import latmod.lib.util.FinalIDObject;
import serverutils.lib.api.block.IBlockLM;
import serverutils.lib.api.item.IItemLM;
import serverutils.lib.api.recipes.LMRecipes;
import serverutils.lib.api.tile.TileLM;
import serverutils.lib.mod.ServerUtilitiesLibraryMod;

public class LMMod extends FinalIDObject {

    public static LMMod create(String s) {
        LMMod mod = new LMMod(s);
        if (ServerUtilitiesLib.DEV_ENV) ServerUtilitiesLib.dev_logger.info("LMMod '" + mod.getID() + "' created");
        return mod;
    }

    // End of static //

    public final String lowerCaseModID;
    private ModContainer modContainer;
    public final List<IItemLM> itemsAndBlocks;

    public LMRecipes recipes;

    public LMMod(String id) {
        super(id);
        lowerCaseModID = id.toLowerCase();
        itemsAndBlocks = new ArrayList<>();

        recipes = LMRecipes.defaultInstance;
    }

    public ModContainer getModContainer() {
        if (modContainer == null) modContainer = Loader.instance().getModObjectList().inverse().get(getID());
        return modContainer;
    }

    public void setRecipes(LMRecipes r) {
        recipes = (r == null) ? new LMRecipes() : r;
    }

    public String toFullID() {
        return getID() + '-' + Loader.MC_VERSION + '-' + modContainer.getDisplayVersion();
    }

    public CreativeTabs createTab(final String s, final ItemStack icon) {
        CreativeTabs tab = new CreativeTabs(lowerCaseModID + '.' + s) {

            @SideOnly(Side.CLIENT)
            public ItemStack getIconItemStack() {
                return icon;
            }

            @SideOnly(Side.CLIENT)
            public Item getTabIconItem() {
                return getIconItemStack().getItem();
            }

            @SideOnly(Side.CLIENT)
            public void displayAllReleventItems(List l) {
                for (IItemLM i : itemsAndBlocks) {
                    Item item = i.getItem();
                    if (item.getCreativeTab() == this) item.getSubItems(item, this, l);
                }
            }
        };

        return tab;
    }

    public String getBlockName(String s) {
        return lowerCaseModID + ".tile." + s;
    }

    public String getItemName(String s) {
        return lowerCaseModID + ".item." + s;
    }

    public String translate(String s, Object... args) {
        return ServerUtilitiesLibraryMod.proxy.translate(lowerCaseModID + '.' + s, args);
    }

    public void addItem(IItemLM... ai) {
        for (IItemLM i : ai) {
            if (i instanceof IBlockLM) {
                ServerUtilitiesLib.addBlock((Block) i, ((IBlockLM) i).getItemBlock(), i.getItemID());
            } else {
                ServerUtilitiesLib.addItem(i.getItem(), i.getItemID());
            }

            itemsAndBlocks.add(i);
        }
    }

    public void addTile(Class<? extends TileLM> c, String s, String... alt) {
        ServerUtilitiesLib.addTileEntity(c, getID() + '.' + s, alt);
    }

    public void addEntity(Class<? extends Entity> c, String s, int id) {
        ServerUtilitiesLib.addEntity(c, s, id, getID());
    }

    public void onPostLoaded() {
        for (IItemLM i : itemsAndBlocks) i.onPostLoaded();
    }

    public void loadRecipes() {
        for (IItemLM i : itemsAndBlocks) i.loadRecipes();
    }

    public IChatComponent chatComponent(String s, Object... obj) {
        return new ChatComponentTranslation(lowerCaseModID + '.' + s, obj);
    }
}
