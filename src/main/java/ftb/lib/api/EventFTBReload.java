package ftb.lib.api;

import net.minecraft.command.ICommandSender;

import ftb.lib.FTBWorld;

public class EventFTBReload extends EventLM {

    public final FTBWorld world;
    public final ICommandSender sender;
    public final boolean reloadingClient;

    public EventFTBReload(FTBWorld w, ICommandSender ics, boolean b) {
        world = w;
        sender = ics;
        reloadingClient = b;
    }
}
