package serverutils.aurora;

import java.util.Locale;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import serverutils.aurora.mc.AuroraMinecraftHandler;

@Mod(modid = Aurora.MOD_ID, name = Aurora.MOD_NAME, version = Aurora.VERSION, acceptableRemoteVersions = "*")
public class Aurora {

    public static final String MOD_ID = "aurora";
    public static final String MOD_NAME = "Server Utilities Aurora";
    public static final String VERSION = "GRADLETOKEN_VERSION";
    private static AuroraServer server;

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
        Locale.setDefault(Locale.US);
        AuroraConfig.init(event);
        MinecraftForge.EVENT_BUS.register(AuroraConfig.INST);
        MinecraftForge.EVENT_BUS.register(AuroraMinecraftHandler.INST);
        FMLCommonHandler.instance().bus().register(AuroraMinecraftHandler.INST);
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        if (AuroraConfig.general.enable) {
            start(event.getServer());
        }
    }

    @Mod.EventHandler
    public void onServerStopping(FMLServerStoppingEvent event) {
        if (AuroraConfig.general.enable) {
            stop();
        }
    }

    public static void start(MinecraftServer s) {
        if (AuroraConfig.general.enable) {
            if (server == null) {
                server = new AuroraServer(s, AuroraConfig.general.port);
                server.start();
            }
        }
    }

    public static void stop() {
        if (AuroraConfig.general.enable) {
            if (server != null) {
                server.shutdown();
                server = null;
            }
        }
    }
}
