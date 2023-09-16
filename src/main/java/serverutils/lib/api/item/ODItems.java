package serverutils.lib.api.item;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.init.*;
import net.minecraft.item.*;
import net.minecraftforge.oredict.OreDictionary;

public class ODItems {

    public static final int ANY = OreDictionary.WILDCARD_VALUE;

    public static final String WOOD = "logWood";
    public static final String SAPLING = "treeSapling";
    public static final String PLANKS = "plankWood";
    public static final String STICK = "stickWood";
    public static final String GLASS = "blockGlassColorless";
    public static final String GLASS_ANY = "blockGlass";
    public static final String GLASS_PANE = "paneGlassColorless";
    public static final String GLASS_PANE_ANY = "paneGlass";
    public static final String STONE = "stone";
    public static final String COBBLE = "cobblestone";
    public static final String SAND = "sand";
    public static final ItemStack OBSIDIAN = new ItemStack(Blocks.obsidian);
    public static final ItemStack WOOL = new ItemStack(Blocks.wool, 1, ANY);
    public static final ItemStack WOOL_WHITE = new ItemStack(Blocks.wool, 1, 0);

    public static final String SLIMEBALL = "slimeball";
    public static final String MEAT_RAW = "listAllmeatraw";
    public static final String MEAT_COOKED = "listAllmeatcooked";
    public static final String RUBBER = "itemRubber";
    public static final String SILICON = "itemSilicon";

    public static final String REDSTONE = "dustRedstone";
    public static final String GLOWSTONE = "dustGlowstone";
    public static final String QUARTZ = "gemQuartz";
    public static final String LAPIS = "gemLapis";

    public static final String IRON = "ingotIron";
    public static final String GOLD = "ingotGold";
    public static final String DIAMOND = "gemDiamond";
    public static final String EMERALD = "gemEmerald";

    public static final String TIN = "ingotTin";
    public static final String COPPER = "ingotCopper";
    public static final String LEAD = "ingotLead";
    public static final String BRONZE = "ingotBronze";
    public static final String SILVER = "ingotSilver";

    public static final String RUBY = "gemRuby";
    public static final String SAPPHIRE = "gemSapphire";
    public static final String PERIDOT = "gemPeridot";

    public static final String NUGGET_GOLD = "nuggetGold";
    public static final String NUGGET_TIN = "nuggetTin";
    public static final String NUGGET_COPPER = "nuggetCopper";
    public static final String NUGGET_LEAD = "nuggetLead";
    public static final String NUGGET_SILVER = "nuggetSilver";

    public static final class OreStackEntry {

        public final ItemStack itemStack;
        public final String oreName;

        public OreStackEntry(ItemStack is, String s) {
            itemStack = is;
            oreName = s;
        }

        public boolean equals(Object o) {
            ItemStack is = (o == null) ? null
                    : ((o instanceof OreStackEntry) ? ((OreStackEntry) o).itemStack : (ItemStack) o);
            return is.getItem() == itemStack.getItem()
                    && (is.getItemDamage() == itemStack.getItemDamage() || itemStack.getItemDamage() == ANY);
        }
    }

    public static void preInit() {
        add(MEAT_RAW, new ItemStack(Items.beef));
        add(MEAT_RAW, new ItemStack(Items.porkchop));
        add(MEAT_RAW, new ItemStack(Items.chicken));

        add(MEAT_COOKED, new ItemStack(Items.cooked_beef));
        add(MEAT_COOKED, new ItemStack(Items.cooked_porkchop));
        add(MEAT_COOKED, new ItemStack(Items.cooked_chicken));
    }

    public static void postInit() {
        // Item wrench = LMInvUtils.getItemFromRegName(new ResourceLocation(OtherMods.THERMAL_EXPANSION, "wrench"));
        // if(wrench != null) wrench.setHarvestLevel(Tool.Type.WRENCH, Tool.Level.BASIC);
    }

    private static boolean addOreName(String item, int damage, String name) {
        Item i = LMInvUtils.getItemFromRegName(item);
        if (i != null) add(name, new ItemStack(i, 1, damage));
        return i != null;
    }

    public static ItemStack add(String name, ItemStack is) {
        ItemStack is1 = LMInvUtils.singleCopy(is);
        OreDictionary.registerOre(name, is1);
        return is1;
    }

    public static List<String> getOreNames(ItemStack is) {
        int[] ai = OreDictionary.getOreIDs(is);
        if (ai == null || ai.length == 0) return new ArrayList<>();
        ArrayList<String> l = new ArrayList<>();
        for (int i : ai) l.add(OreDictionary.getOreName(i));
        return l;
    }

    public static List<ItemStack> getOres(String name) {
        return OreDictionary.getOres(name);
    }

    public static ItemStack getFirstOre(String name) {
        List<ItemStack> l = getOres(name);
        if (!l.isEmpty()) return l.get(0);
        return null;
    }

    public static boolean hasOre(String s) {
        return !getOres(s).isEmpty();
    }
}
