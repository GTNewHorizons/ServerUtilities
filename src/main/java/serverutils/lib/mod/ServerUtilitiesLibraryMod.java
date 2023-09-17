package serverutils.lib.mod;

import java.io.File;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.relauncher.Side;
import latmod.lib.util.OS;
import serverutils.lib.EventBusHelper;
import serverutils.lib.JsonHelper;
import serverutils.lib.LMAccessToken;
import serverutils.lib.LMMod;
import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.ServerUtilsWorld;
import serverutils.lib.api.EventServerUtilitiesWorldServer;
import serverutils.lib.api.GameModes;
import serverutils.lib.api.config.ConfigRegistry;
import serverutils.lib.api.item.ODItems;
import serverutils.lib.mod.cmd.CmdEditConfig;
import serverutils.lib.mod.cmd.CmdHelpOverride;
import serverutils.lib.mod.cmd.CmdListOverride;
import serverutils.lib.mod.cmd.CmdMode;
import serverutils.lib.mod.cmd.CmdNotify;
import serverutils.lib.mod.cmd.CmdReload;
import serverutils.lib.mod.cmd.CmdSetItemName;
import serverutils.lib.mod.cmd.CmdTrashCan;
import serverutils.lib.mod.config.ServerUtilitiesLibConfigCmd;
import serverutils.lib.mod.config.ServerUtilitiesLibraryConfig;
import serverutils.lib.mod.net.ServerUtilitiesLibraryLibNetHandler;

@Mod(
        modid = ServerUtilitiesLibFinals.MOD_ID,
        name = ServerUtilitiesLibFinals.MOD_NAME,
        version = ServerUtilitiesLibFinals.MOD_VERSION,
        dependencies = ServerUtilitiesLibFinals.MOD_DEP,
        acceptedMinecraftVersions = "1.7.10")
public class ServerUtilitiesLibraryMod {

    @Mod.Instance(ServerUtilitiesLibFinals.MOD_ID)
    public static ServerUtilitiesLibraryMod inst;

    @SidedProxy(
            serverSide = "serverutils.lib.mod.ServerUtilitiesLibraryModCommon",
            clientSide = "serverutils.lib.mod.client.ServerUtilitiesLibraryModClient")
    public static ServerUtilitiesLibraryModCommon proxy;

    public static final Logger logger = LogManager.getLogger("ServerUtilsLibrary");

    public static LMMod mod;

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent e) {
        if (ServerUtilitiesLib.DEV_ENV) logger.info("Loading ServerUtilitiesLibrary, DevEnv");
        else logger.info("Loading ServerUtilitiesLibrary, v" + ServerUtilitiesLibFinals.MOD_VERSION);

        logger.info("OS: " + OS.current + ", 64bit: " + OS.is64);

        mod = LMMod.create(ServerUtilitiesLibFinals.MOD_ID);

        ServerUtilitiesLib.init(e.getModConfigurationDirectory());
        JsonHelper.init();
        ServerUtilitiesLibraryLibNetHandler.init();
        ODItems.preInit();

        ServerUtilitiesLibraryConfig.load();
        EventBusHelper.register(new ServerUtilitiesLibEventHandler());
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        FMLInterModComms
                .sendMessage("Waila", "register", "serverutils.lib.api.waila.EventRegisterWaila.registerHandlers");
    }

    @Mod.EventHandler
    public void onPostInit(FMLPostInitializationEvent e) {
        ODItems.postInit();
        proxy.postInit();
        GameModes.reload();
        ConfigRegistry.reload();
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent e) {
        if (ServerUtilitiesLibConfigCmd.override_list.getAsBoolean())
            ServerUtilitiesLib.addCommand(e, new CmdListOverride());
        if (ServerUtilitiesLibConfigCmd.override_help.getAsBoolean())
            ServerUtilitiesLib.addCommand(e, new CmdHelpOverride());
        ServerUtilitiesLib.addCommand(e, new CmdEditConfig());
        ServerUtilitiesLib.addCommand(e, new CmdMode());
        ServerUtilitiesLib.addCommand(e, new CmdReload());
        ServerUtilitiesLib.addCommand(e, new CmdNotify());
        ServerUtilitiesLib.addCommand(e, new CmdSetItemName());
        ServerUtilitiesLib.addCommand(e, new CmdTrashCan());
    }

    @Mod.EventHandler
    public void onServerAboutToStart(FMLServerAboutToStartEvent e) {
        ServerUtilitiesLib.folderWorld = new File(
                FMLCommonHandler.instance().getSavesDirectory(),
                e.getServer().getFolderName());
        ConfigRegistry.reload();

        GameModes.reload();
        ServerUtilsWorld.server = new ServerUtilsWorld(Side.SERVER);
        EventServerUtilitiesWorldServer event = new EventServerUtilitiesWorldServer(
                ServerUtilsWorld.server,
                e.getServer());
        if (ServerUtilitiesLib.serverUtilitiesIntegration != null)
            ServerUtilitiesLib.serverUtilitiesIntegration.onServerUtilitiesWorldServer(event);
        event.post();
    }

    @Mod.EventHandler
    public void onServerStarted(FMLServerStartedEvent e) {
        ServerUtilitiesLib.reload(ServerUtilitiesLib.getServer(), false, false);
    }

    @Mod.EventHandler
    public void onServerShutDown(FMLServerStoppedEvent e) {
        if (ServerUtilitiesLib.serverUtilitiesIntegration != null)
            ServerUtilitiesLib.serverUtilitiesIntegration.onServerUtilitiesWorldServerClosed();
        ServerUtilsWorld.server = null;
        ServerUtilitiesLib.folderWorld = null;
        LMAccessToken.clear();
        ConfigRegistry.clearTemp();
        // ServerUtilitiesLibraryEventHandler.loaded = false;
    }

    @NetworkCheckHandler
    public boolean checkNetwork(Map<String, String> m, Side side) {
        String s = m.get(ServerUtilitiesLibFinals.MOD_ID);
        return s == null || s.equals(ServerUtilitiesLibFinals.MOD_VERSION);
    }
}
