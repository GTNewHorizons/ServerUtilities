package ftb.lib.api;

import net.minecraft.server.MinecraftServer;

import ftb.lib.FTBWorld;

public class EventFTBWorldServer extends EventLM {

    public final FTBWorld world;
    public final MinecraftServer server;

    public EventFTBWorldServer(FTBWorld w, MinecraftServer s) {
        world = w;
        server = s;
    }
}
