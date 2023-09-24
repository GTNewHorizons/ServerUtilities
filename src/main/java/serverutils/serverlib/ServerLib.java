package serverutils.serverlib;

import serverutils.serverlib.command.CmdAddFakePlayer;
import serverutils.serverlib.command.CmdMySettings;
import serverutils.serverlib.command.CmdReload;
import serverutils.serverlib.command.team.CmdTeam;
import serverutils.serverlib.lib.command.CommandUtils;
import serverutils.serverlib.lib.data.Universe;
import serverutils.serverlib.lib.util.SidedUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.Item;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.util.IChatComponent;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;

@Mod(
		modid = ServerLib.MOD_ID,
		name = ServerLib.MOD_NAME,
		version = ServerLib.VERSION,
		acceptableRemoteVersions = "*",
		dependencies = "required-after:forge@[0.0.0.forge,);after:nei@[4.6.0,);"
)
public class ServerLib
{
	public static final String MOD_ID = "Serverlib";
	public static final String MOD_NAME = "ServerUtils Library";
	public static final String VERSION = "0.0.0.serverlib";
	public static final String THIS_DEP = "required-after:" + MOD_ID + "@[" + VERSION + ",)";
	public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);
	public static final String KEY_CATEGORY = "key.categories.servermods";

	static
	{
		//Caution, hacky code! This fixes JavaFX not working outside DevEnv, but also excludes it from Forge classloader and prevents core mods from touching it. Fixed by modmuss50.
		ClassLoader classLoader = ServerLib.class.getClassLoader();

		if (classLoader instanceof LaunchClassLoader)
		{
			((LaunchClassLoader) classLoader).addClassLoaderExclusion("javafx.");
		}
	}

	@SidedProxy(serverSide = "serverutils.serverlib.ServerLibCommon", clientSide = "client.serverutils.ServerLibClient")
	public static ServerLibCommon PROXY;

	@GameRegistry.ObjectHolder("ftbquests:custom_icon")
	public static Item CUSTOM_ICON_ITEM;

	public static IChatComponent lang(@Nullable ICommandSender sender, String key, Object... args)
	{
		return SidedUtils.lang(sender, MOD_ID, key, args);
	}

	public static CommandException error(@Nullable ICommandSender sender, String key, Object... args)
	{
		return CommandUtils.error(lang(sender, key, args));
	}

	public static CommandException errorFeatureDisabledServer(@Nullable ICommandSender sender)
	{
		return error(sender, "feature_disabled_server");
	}

	@Mod.EventHandler
	public void onPreInit(FMLPreInitializationEvent event)
	{
		Locale.setDefault(Locale.US);
		ServerLibConfig.sync();
		PROXY.preInit(event);
	}

	@Mod.EventHandler
	public void onPostInit(FMLPostInitializationEvent event)
	{
		PROXY.postInit();
	}

	@Mod.EventHandler
	public void onServerAboutToStart(FMLServerAboutToStartEvent event)
	{
		Universe.onServerAboutToStart(event);
	}

	@Mod.EventHandler
	public void onServerStarting(FMLServerStartingEvent event)
	{
		event.registerServerCommand(new CmdReload());
		event.registerServerCommand(new CmdMySettings());
		event.registerServerCommand(new CmdTeam());

		if (ServerLibConfig.debugging.special_commands)
		{
			event.registerServerCommand(new CmdAddFakePlayer());
		}
	}

	@Mod.EventHandler
	public void onServerStarted(FMLServerStartedEvent event)
	{
		Universe.onServerStarted(event);
	}

	@Mod.EventHandler
	public void onServerStopping(FMLServerStoppingEvent event)
	{
		Universe.onServerStopping(event);
	}

	@NetworkCheckHandler
	public boolean checkModLists(Map<String, String> map, Side side)
	{
		SidedUtils.checkModLists(side, map);
		return true;
	}
}