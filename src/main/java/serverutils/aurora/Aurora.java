package serverutils.aurora;

import net.minecraft.server.MinecraftServer;

public class Aurora {

    private static AuroraServer server;

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
