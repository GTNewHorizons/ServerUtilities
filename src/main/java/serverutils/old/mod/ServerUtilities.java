package serverutils.old.mod;

import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import serverutils.old.mod.cmd.CmdBack;
import serverutils.old.mod.cmd.CmdDelHome;
import serverutils.old.mod.cmd.CmdHome;
import serverutils.old.mod.cmd.CmdLMPlayerSettings;
import serverutils.old.mod.cmd.CmdSetHome;
import serverutils.old.mod.cmd.CmdSpawn;
import serverutils.old.mod.cmd.CmdTplast;
import serverutils.old.mod.cmd.CmdWarp;
import serverutils.old.mod.cmd.admin.CmdAdmin;
import serverutils.old.mod.config.ServerUtilitiesConfig;
import serverutils.old.mod.config.ServerUtilitiesConfigCmd;
import serverutils.old.mod.handlers.ServerUtilitiesChatEventHandler;
import serverutils.old.mod.handlers.ServerUtilitiesChunkEventHandler;
import serverutils.old.mod.handlers.ServerUtilitiesPlayerEventHandler;
import serverutils.old.mod.handlers.ServerUtiltiesWorldEventHandler;
import serverutils.old.net.ServerUtilitiesNetHandler;
import serverutils.old.world.Backups;

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
            clientSide = "serverutils.old.mod.client.ServerUtilitiesClient",
            serverSide = "serverutils.old.mod.ServerUtilitiesCommon")
    public static ServerUtilitiesCommon proxy;

    @SidedProxy(
            clientSide = "serverutils.old.mod.handlers.serverlib.ServerUtilitiesLibraryIntegrationClient",
            serverSide = "serverutils.old.mod.handlers.serverlib.ServerUtilitiesLibraryIntegration")
    public static serverutils.old.mod.handlers.serverlib.ServerUtilitiesLibraryIntegration serverutillib_int;

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
