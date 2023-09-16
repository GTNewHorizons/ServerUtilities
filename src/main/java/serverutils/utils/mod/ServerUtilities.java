package serverutils.utils.mod;

import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.relauncher.Side;
import serverutils.lib.EventBusHelper;
import serverutils.lib.LMMod;
import serverutils.lib.ServerUtilitiesLib;
import serverutils.utils.mod.cmd.CmdBack;
import serverutils.utils.mod.cmd.CmdDelHome;
import serverutils.utils.mod.cmd.CmdHome;
import serverutils.utils.mod.cmd.CmdLMPlayerSettings;
import serverutils.utils.mod.cmd.CmdSetHome;
import serverutils.utils.mod.cmd.CmdSpawn;
import serverutils.utils.mod.cmd.CmdTplast;
import serverutils.utils.mod.cmd.CmdWarp;
import serverutils.utils.mod.cmd.admin.CmdAdmin;
import serverutils.utils.mod.config.ServerUtilitiesConfig;
import serverutils.utils.mod.config.ServerUtilitiesConfigCmd;
import serverutils.utils.mod.handlers.ServerUtilitiesChatEventHandler;
import serverutils.utils.mod.handlers.ServerUtilitiesChunkEventHandler;
import serverutils.utils.mod.handlers.ServerUtilitiesPlayerEventHandler;
import serverutils.utils.mod.handlers.ServerUtiltiesWorldEventHandler;
import serverutils.utils.net.ServerUtilitiesNetHandler;
import serverutils.utils.world.Backups;

@Mod(
        modid = ServerUtilitiesFinals.MOD_ID,
        version = ServerUtilitiesFinals.MOD_VERSION,
        name = ServerUtilitiesFinals.MOD_NAME,
        dependencies = ServerUtilitiesFinals.MOD_DEP,
        acceptedMinecraftVersions = "1.7.10")
public class ServerUtilities {

    @Mod.Instance(ServerUtilitiesFinals.MOD_ID)
    public static ServerUtilities inst;

    @SidedProxy(
            clientSide = "serverutils.utils.mod.client.ServerUtilitiesClient",
            serverSide = "serverutils.utils.mod.ServerUtilitiesCommon")
    public static ServerUtilitiesCommon proxy;

    @SidedProxy(
            clientSide = "serverutils.utils.mod.handlers.serverlib.ServerUtilitiesLibraryIntegrationClient",
            serverSide = "serverutils.utils.mod.handlers.serverlib.ServerUtilitiesLibraryIntegration")
    public static serverutils.utils.mod.handlers.serverlib.ServerUtilitiesLibraryIntegration serverutillib_int;

    public static LMMod mod;

    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        mod = LMMod.create(ServerUtilitiesFinals.MOD_ID);
        ServerUtilitiesLib.serverUtilitiesIntegration = serverutillib_int;
        logger = LogManager.getLogger(ServerUtilitiesFinals.MOD_ID);

        ServerUtilitiesConfig.load();

        EventBusHelper.register(new ServerUtilitiesPlayerEventHandler());
        EventBusHelper.register(new ServerUtiltiesWorldEventHandler());
        EventBusHelper.register(new ServerUtilitiesChatEventHandler());
        ServerUtilitiesChunkEventHandler.instance.init();

        ServerUtilitiesNetHandler.init();
        Backups.init();
        proxy.preInit();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent e) {
        proxy.postInit();
    }

    @Mod.EventHandler
    public void registerCommands(FMLServerStartingEvent e) {
        ServerUtilitiesLib.addCommand(e, new CmdAdmin());
        ServerUtilitiesLib.addCommand(e, new CmdLMPlayerSettings());

        if (ServerUtilitiesConfigCmd.back.getAsBoolean()) ServerUtilitiesLib.addCommand(e, new CmdBack());
        if (ServerUtilitiesConfigCmd.spawn.getAsBoolean()) ServerUtilitiesLib.addCommand(e, new CmdSpawn());
        if (ServerUtilitiesConfigCmd.tplast.getAsBoolean()) ServerUtilitiesLib.addCommand(e, new CmdTplast());
        if (ServerUtilitiesConfigCmd.warp.getAsBoolean()) ServerUtilitiesLib.addCommand(e, new CmdWarp());

        if (ServerUtilitiesConfigCmd.home.getAsBoolean()) {
            ServerUtilitiesLib.addCommand(e, new CmdSetHome());
            ServerUtilitiesLib.addCommand(e, new CmdHome());
            ServerUtilitiesLib.addCommand(e, new CmdDelHome());
        }
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent e) {
        if (ServerUtilitiesLib.hasOnlinePlayers()) {
            for (EntityPlayerMP ep : ServerUtilitiesLib.getAllOnlinePlayers(null)) {
                ServerUtilitiesPlayerEventHandler.playerLoggedOut(ep);
            }
        }
    }

    @NetworkCheckHandler
    public boolean checkNetwork(Map<String, String> m, Side side) {
        String s = m.get(ServerUtilitiesFinals.MOD_ID);
        return s == null || s.equals(ServerUtilitiesFinals.MOD_VERSION);
    }
}
