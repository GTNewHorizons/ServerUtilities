package serverutils.lib.client;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import serverutils.lib.ServerUtilitiesLib;

public class ServerUtilitiesLibClientConfig {

    public static Configuration config;
    public static final ServerUtilitiesLibClientConfig INST = new ServerUtilitiesLibClientConfig();

    public static void init(FMLPreInitializationEvent event) {
        config = new Configuration(
                new File(event.getModConfigurationDirectory() + "/../server utilities/client/serverutlitieslib.cfg"));
        sync();
    }

    public static boolean sync() {
        config.load();

        item_ore_names = config.get(
                Configuration.CATEGORY_GENERAL,
                "item_ore_names",
                false,
                "Show item Ore Dictionary names in inventory.").getBoolean();

        item_nbt = config.get(Configuration.CATEGORY_GENERAL, "item_nbt", false, "Show item NBT in inventory.")
                .getBoolean();

        debug_helper = config
                .get(Configuration.CATEGORY_GENERAL, "debug_helper", true, "Show help text while holding F3.")
                .getBoolean();

        replace_vanilla_status_messages = config
                .get(
                        Configuration.CATEGORY_GENERAL,
                        "replace_vanilla_status_messages",
                        true,
                        "Replace vanilla status message with Notifications, which support colors and timers.")
                .getBoolean();

        action_buttons = EnumSidebarButtonPlacement.string2placement(
                config.get(
                        Configuration.CATEGORY_GENERAL,
                        "action_buttons",
                        "auto",
                        "DISABLED: Buttons are hidden;\nTOP_LEFT: Buttons are placed on top-left corner, where NEI has it's buttons;\nINVENTORY_SIDE: Buttons are placed on the side or top of your inventory, depending on potion effects and crafting book;\nAUTO: When NEI is installed, INVENTORY_SIDE, else TOP_LEFT.")
                        .getString());

        config.save();

        return true;
    }

    public static boolean item_ore_names;
    public static boolean item_nbt;
    public static EnumSidebarButtonPlacement action_buttons;
    public static boolean replace_vanilla_status_messages;
    public static boolean debug_helper;

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.modID.equals(ServerUtilitiesLib.MOD_ID)) {
            sync();
        }
    }
}
