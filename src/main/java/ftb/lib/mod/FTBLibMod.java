package ftb.lib.mod;

import java.io.File;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.relauncher.Side;
import ftb.lib.EventBusHelper;
import ftb.lib.FTBLib;
import ftb.lib.FTBWorld;
import ftb.lib.JsonHelper;
import ftb.lib.LMAccessToken;
import ftb.lib.LMMod;
import ftb.lib.api.EventFTBWorldServer;
import ftb.lib.api.GameModes;
import ftb.lib.api.config.ConfigRegistry;
import ftb.lib.api.item.ODItems;
import ftb.lib.mod.cmd.CmdEditConfig;
import ftb.lib.mod.cmd.CmdHelpOverride;
import ftb.lib.mod.cmd.CmdListOverride;
import ftb.lib.mod.cmd.CmdMode;
import ftb.lib.mod.cmd.CmdNotify;
import ftb.lib.mod.cmd.CmdReload;
import ftb.lib.mod.cmd.CmdSetItemName;
import ftb.lib.mod.cmd.CmdTrashCan;
import ftb.lib.mod.config.FTBLibConfig;
import ftb.lib.mod.config.FTBLibConfigCmd;
import ftb.lib.mod.net.FTBLibNetHandler;
import latmod.lib.util.OS;

@Mod(
        modid = FTBLibFinals.MOD_ID,
        name = FTBLibFinals.MOD_NAME,
        version = FTBLibFinals.MOD_VERSION,
        dependencies = FTBLibFinals.MOD_DEP,
        acceptedMinecraftVersions = "1.7.10")
public class FTBLibMod {

    @Mod.Instance(FTBLibFinals.MOD_ID)
    public static FTBLibMod inst;

    @SidedProxy(serverSide = "ftb.lib.mod.FTBLibModCommon", clientSide = "ftb.lib.mod.client.FTBLibModClient")
    public static FTBLibModCommon proxy;

    public static final Logger logger = LogManager.getLogger("FTBLib");

    public static LMMod mod;

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent e) {
        if (FTBLib.DEV_ENV) logger.info("Loading FTBLib, DevEnv");
        else logger.info("Loading FTBLib, v" + FTBLibFinals.MOD_VERSION);

        logger.info("OS: " + OS.current + ", 64bit: " + OS.is64);

        mod = LMMod.create(FTBLibFinals.MOD_ID);

        FTBLib.init(e.getModConfigurationDirectory());
        JsonHelper.init();
        FTBLibNetHandler.init();
        ODItems.preInit();

        FTBLibConfig.load();
        EventBusHelper.register(new FTBLibEventHandler());
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        FMLInterModComms.sendMessage("Waila", "register", "ftb.lib.api.waila.EventRegisterWaila.registerHandlers");
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
        if (FTBLibConfigCmd.override_list.getAsBoolean()) FTBLib.addCommand(e, new CmdListOverride());
        if (FTBLibConfigCmd.override_help.getAsBoolean()) FTBLib.addCommand(e, new CmdHelpOverride());
        FTBLib.addCommand(e, new CmdEditConfig());
        FTBLib.addCommand(e, new CmdMode());
        FTBLib.addCommand(e, new CmdReload());
        FTBLib.addCommand(e, new CmdNotify());
        FTBLib.addCommand(e, new CmdSetItemName());
        FTBLib.addCommand(e, new CmdTrashCan());
    }

    @Mod.EventHandler
    public void onServerAboutToStart(FMLServerAboutToStartEvent e) {
        FTBLib.folderWorld = new File(FMLCommonHandler.instance().getSavesDirectory(), e.getServer().getFolderName());
        ConfigRegistry.reload();

        GameModes.reload();
        FTBWorld.server = new FTBWorld(Side.SERVER);
        EventFTBWorldServer event = new EventFTBWorldServer(FTBWorld.server, e.getServer());
        if (FTBLib.ftbu != null) FTBLib.ftbu.onFTBWorldServer(event);
        event.post();
    }

    @Mod.EventHandler
    public void onServerStarted(FMLServerStartedEvent e) {
        FTBLib.reload(FTBLib.getServer(), false, false);
    }

    @Mod.EventHandler
    public void onServerShutDown(FMLServerStoppedEvent e) {
        if (FTBLib.ftbu != null) FTBLib.ftbu.onFTBWorldServerClosed();
        FTBWorld.server = null;
        FTBLib.folderWorld = null;
        LMAccessToken.clear();
        ConfigRegistry.clearTemp();
        // FTBLibEventHandler.loaded = false;
    }

    @NetworkCheckHandler
    public boolean checkNetwork(Map<String, String> m, Side side) {
        String s = m.get(FTBLibFinals.MOD_ID);
        return s == null || s.equals(FTBLibFinals.MOD_VERSION);
    }
}
