package serverutils.serverlib.client;

import serverutils.serverlib.ServerLib;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;

/**
 * @author LatvianModder
 */
@Mod.EventBusSubscriber(modid = ServerLib.MOD_ID, value = Side.CLIENT)
@Config(modid = "serverlib_client", name = "../local/client/serverlib")
public class ServerLibClientConfig
{
	@Config.Comment("Show item Ore Dictionary names in inventory.")
	public static boolean item_ore_names = false;

	@Config.Comment("Show item NBT in inventory.")
	public static boolean item_nbt = false;

	@Config.Comment({
			"DISABLED: Buttons are hidden;",
			"TOP_LEFT: Buttons are placed on top-left corner, where NEI has it's buttons;",
			"INVENTORY_SIDE: Buttons are placed on the side or top of your inventory, depending on potion effects and crafting book;",
			"AUTO: When NEI is installed, INVENTORY_SIDE, else TOP_LEFT."
	})
	@Config.LangKey("sidebar_button")
	public static EnumSidebarButtonPlacement action_buttons = EnumSidebarButtonPlacement.AUTO;

	@Config.Comment("Replace vanilla status message with Notifications, which support colors and timers.")
	public static boolean replace_vanilla_status_messages = true;

	@Config.Comment("Show help text while holding F3.")
	public static boolean debug_helper = true;

	public static void sync()
	{
		ConfigManager.sync("serverlib_client", Config.Type.INSTANCE);
	}

	@SubscribeEvent
	public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
	{
		if (event.modID.equals("serverlib_client"))
		{
			sync();
		}
	}
}