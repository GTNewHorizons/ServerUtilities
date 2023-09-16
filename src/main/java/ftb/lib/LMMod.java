package ftb.lib;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.item.*;
import net.minecraft.util.*;

import cpw.mods.fml.common.*;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ftb.lib.api.block.IBlockLM;
import ftb.lib.api.item.IItemLM;
import ftb.lib.api.recipes.LMRecipes;
import ftb.lib.api.tile.TileLM;
import ftb.lib.mod.FTBLibMod;
import latmod.lib.util.FinalIDObject;

public class LMMod extends FinalIDObject {

    public static LMMod create(String s) {
        LMMod mod = new LMMod(s);
        if (FTBLib.DEV_ENV) FTBLib.dev_logger.info("LMMod '" + mod.getID() + "' created");
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
        return FTBLibMod.proxy.translate(lowerCaseModID + '.' + s, args);
    }

    public void addItem(IItemLM... ai) {
        for (IItemLM i : ai) {
            if (i instanceof IBlockLM) {
                FTBLib.addBlock((Block) i, ((IBlockLM) i).getItemBlock(), i.getItemID());
            } else {
                FTBLib.addItem(i.getItem(), i.getItemID());
            }

            itemsAndBlocks.add(i);
        }
    }

    public void addTile(Class<? extends TileLM> c, String s, String... alt) {
        FTBLib.addTileEntity(c, getID() + '.' + s, alt);
    }

    public void addEntity(Class<? extends Entity> c, String s, int id) {
        FTBLib.addEntity(c, s, id, getID());
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
