package serverutils.invsee.inventories;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import cpw.mods.fml.common.Loader;
import serverutils.lib.util.CommonUtils;

@ApiStatus.AvailableSince("2.4.0")
public final class InvSeeRegistry {

    private static final List<IModdedInventory> registeredInventories = new ArrayList<>();

    static {
        // Initializes all default inventories
        // noinspection ResultOfMethodCallIgnored
        DefaultInventories.MAIN.ordinal();
    }

    private enum DefaultInventories {

        // spotless:off
        MAIN(null, MainInventory.class),
        ENDER_CHEST(null, EnderInventory.class),
        ADVENTURE_BACKPACK("adventurebackpack", AdventureBackpackInv.class),
        BAUBLES("Baubles", BaublesInventory.class),
        BATTLE_GEAR("battlegear2", BattlegearInventory.class, () -> CommonUtils.getClassExists("mods.battlegear2.api.core.IInventoryPlayerBattle")),
        BACKPACK("Backpack", MinecraftBackpackInv.class),
        GALACTICRAFT("GalacticraftCore", GalacticraftInventory.class),
        TINKERS_CONSTRUCT("TConstruct", TiCInventory.class),;
        // spotless:on

        DefaultInventories(@Nullable String modId, Class<? extends IModdedInventory> inventory) {
            this(modId, inventory, null);
        }

        DefaultInventories(@Nullable String modId, Class<? extends IModdedInventory> inventory,
                @Nullable Supplier<Boolean> isLoaded) {
            if ((modId == null || Loader.isModLoaded(modId)) && (isLoaded == null || isLoaded.get())) {
                try {
                    registerInventory(inventory.getDeclaredConstructor().newInstance());
                } catch (Exception ignored) {}
            }
        }
    }

    public static void registerInventory(IModdedInventory inventory) {
        registeredInventories.add(inventory);
    }

    public static List<IModdedInventory> getRegisteredInventories() {
        return registeredInventories;
    }

    public static IModdedInventory getMainInventory() {
        return registeredInventories.get(0);
    }
}
