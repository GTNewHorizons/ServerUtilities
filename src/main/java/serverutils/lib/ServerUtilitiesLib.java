package serverutils.lib;

import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.Item;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import serverutils.lib.command.CmdAddFakePlayer;
import serverutils.lib.command.CmdMySettings;
import serverutils.lib.command.CmdReload;
import serverutils.lib.command.team.CmdTeam;
import serverutils.lib.lib.command.CommandUtils;
import serverutils.lib.lib.data.Universe;
import serverutils.lib.lib.util.SidedUtils;

@Mod(
        modid = ServerUtilitiesLib.MOD_ID,
        name = ServerUtilitiesLib.MOD_NAME,
        version = ServerUtilitiesLib.VERSION,
        acceptableRemoteVersions = "*",
        dependencies = "")
public class ServerUtilitiesLib {

    public static final String MOD_ID = "Serverlib";
    public static final String MOD_NAME = "ServerUtilitiesLibrary";
    public static final String VERSION = "GRADLETOKEN_VERSION";
    public static final String THIS_DEP = "required-after:" + MOD_ID + "@[" + VERSION + ",)";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);
    public static final String KEY_CATEGORY = "key.categories.servermods";

    static {
        // Caution, hacky code! This fixes JavaFX not working outside DevEnv, but also excludes it from Forge
        // classloader and prevents core mods from touching it. Fixed by modmuss50.
        ClassLoader classLoader = ServerUtilitiesLib.class.getClassLoader();

        if (classLoader instanceof LaunchClassLoader) {
            ((LaunchClassLoader) classLoader).addClassLoaderExclusion("javafx.");
        }
    }

    @SidedProxy(
            serverSide = "serverutils.lib.ServerUtilitiesLibCommon",
            clientSide = "serverutils.lib.client.ServerUtilitiesLibClient")
    public static ServerUtilitiesLibCommon PROXY;

    @GameRegistry.ObjectHolder("serverlibquests:custom_icon")
    public static Item CUSTOM_ICON_ITEM;

    public static IChatComponent lang(@Nullable ICommandSender sender, String key, Object... args) {
        return SidedUtils.lang(sender, MOD_ID, key, args);
    }

    public static CommandException error(@Nullable ICommandSender sender, String key, Object... args) {
        return CommandUtils.error(lang(sender, key, args));
    }

    public static CommandException errorFeatureDisabledServer(@Nullable ICommandSender sender) {
        return error(sender, "feature_disabled_server");
    }

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
        Locale.setDefault(Locale.US);
        ServerUtilitiesLibConfig.sync();
        PROXY.preInit(event);
        MinecraftForge.EVENT_BUS.register(ServerUtilitiesLibConfig.INST);
        MinecraftForge.EVENT_BUS.register(ServerUtilitiesLibEventHandler.INST);
        FMLCommonHandler.instance().bus().register(ServerUtilitiesLibEventHandler.INST);
    }

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        PROXY.init(event);
    }

    @Mod.EventHandler
    public void onPostInit(FMLPostInitializationEvent event) {
        PROXY.postInit();
    }

    @Mod.EventHandler
    public void onServerAboutToStart(FMLServerAboutToStartEvent event) {
        Universe.onServerAboutToStart(event);
        MinecraftForge.EVENT_BUS.register(Universe.get());
        FMLCommonHandler.instance().bus().register(Universe.get());
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CmdReload());
        event.registerServerCommand(new CmdMySettings());
        event.registerServerCommand(new CmdTeam());

        if (ServerUtilitiesLibConfig.debugging.special_commands) {
            event.registerServerCommand(new CmdAddFakePlayer());
        }
    }

    @Mod.EventHandler
    public void onServerStarted(FMLServerStartedEvent event) {
        Universe.onServerStarted(event);
    }

    @Mod.EventHandler
    public void onServerStopping(FMLServerStoppingEvent event) {
        Universe.onServerStopping(event);
    }

    @NetworkCheckHandler
    public boolean checkModLists(Map<String, String> map, Side side) {
        SidedUtils.checkModLists(side, map);
        return true;
    }
}
