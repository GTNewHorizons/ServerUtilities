package serverutils.invsee.inventories;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import cpw.mods.fml.common.Loader;

public enum InvSeeInventories {

    MAIN(null, MainInventory.class),
    ENDER_CHEST(null, EnderInventory.class),
    ADVENTURE_BACKPACK("adventurebackpack", AdventureBackpackInv.class),
    BAUBLES("Baubles", BaublesInventory.class),
    BATTLE_GEAR("battlegear2", BattlegearInventory.class),
    BACKPACK("Backpack", MinecraftBackpackInv.class),
    GALACTICRAFT("GalacticraftCore", GalacticraftInventory.class),
    TINKERS_CONSTRUCT("TConstruct", TiCInventory.class),;

    public static final InvSeeInventories[] VALUES = values();

    private final String modId;
    private final Class<? extends IModdedInventory> inventory;
    private IModdedInventory instance;
    private final boolean loaded;

    private static List<InvSeeInventories> inventories;

    InvSeeInventories(@Nullable String modId, Class<? extends IModdedInventory> inventory) {
        this.modId = modId;
        this.inventory = inventory;
        this.loaded = modId == null || Loader.isModLoaded(modId);
    }

    public @Nullable IModdedInventory getNullableInventory() {
        if (loaded && instance == null) {
            try {
                instance = inventory.getDeclaredConstructor().newInstance();
            } catch (Exception ignored) {}
        }
        return instance;
    }

    public IModdedInventory getInventory() {
        if (!loaded) {
            throw new IllegalStateException("Trying to get IModdedInventory for unloaded mod: " + modId);
        }

        if (instance == null) {
            try {
                instance = inventory.getDeclaredConstructor().newInstance();
            } catch (Exception ignored) {}
        }
        return instance;
    }

    public static List<InvSeeInventories> getActiveInventories() {
        if (inventories == null) {
            inventories = new ArrayList<>();
            for (InvSeeInventories inv : VALUES) {
                if (inv.loaded) {
                    inventories.add(inv);
                }
            }
        }
        return inventories;
    }
}
