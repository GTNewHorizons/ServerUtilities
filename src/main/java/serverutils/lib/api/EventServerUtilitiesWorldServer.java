package serverutils.lib.api;

import net.minecraft.server.MinecraftServer;

import serverutils.lib.ServerUtilsWorld;

public class EventServerUtilitiesWorldServer extends EventLM {

    public final ServerUtilsWorld world;
    public final MinecraftServer server;

    public EventServerUtilitiesWorldServer(ServerUtilsWorld w, MinecraftServer s) {
        world = w;
        server = s;
    }
}
