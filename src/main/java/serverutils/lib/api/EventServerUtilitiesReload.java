package serverutils.lib.api;

import net.minecraft.command.ICommandSender;

import serverutils.lib.ServerUtilsWorld;

public class EventServerUtilitiesReload extends EventLM {

    public final ServerUtilsWorld world;
    public final ICommandSender sender;
    public final boolean reloadingClient;

    public EventServerUtilitiesReload(ServerUtilsWorld w, ICommandSender ics, boolean b) {
        world = w;
        sender = ics;
        reloadingClient = b;
    }
}
