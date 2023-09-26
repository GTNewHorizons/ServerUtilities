package serverutils.aurora;

import java.util.Locale;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;

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
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        start(event.getServer());
    }

    @Mod.EventHandler
    public void onServerStopping(FMLServerStoppingEvent event) {
        stop();
    }

    public static void start(MinecraftServer s) {
        if (server == null) {
            if (AuroraConfig.General.enable) {
                server = new AuroraServer(s, AuroraConfig.General.port);
                server.start();
            }
        }
    }

    public static void stop() {
        if (server != null) {
            server.shutdown();
            server = null;
        }
    }
}
