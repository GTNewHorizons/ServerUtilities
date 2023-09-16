package serverutils.lib.mod.net;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.network.simpleimpl.*;
import cpw.mods.fml.relauncher.Side;
import latmod.lib.ByteCount;
import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.ServerUtilsWorld;
import serverutils.lib.api.EventServerUtilitiesWorldClient;
import serverutils.lib.api.net.LMNetworkWrapper;
import serverutils.lib.api.net.MessageLM;

public class MessageSendWorldID extends MessageLM {

    public MessageSendWorldID() {
        super(ByteCount.INT);
    }

    public MessageSendWorldID(ServerUtilsWorld w, EntityPlayerMP ep) {
        this();
        MessageReload.writeSyncedConfig(io);
        io.writeBoolean(ServerUtilitiesLib.serverUtilitiesIntegration != null);
        w.writeReloadData(io);
        if (ServerUtilitiesLib.serverUtilitiesIntegration != null)
            ServerUtilitiesLib.serverUtilitiesIntegration.writeWorldData(io, ep);
    }

    public LMNetworkWrapper getWrapper() {
        return ServerUtilitiesLibraryLibNetHandler.NET;
    }

    public IMessage onMessage(MessageContext ctx) {
        MessageReload.readSyncedConfig(io);
        boolean hasServerUtilities = io.readBoolean();

        boolean first = ServerUtilsWorld.client == null;
        if (first) ServerUtilsWorld.client = new ServerUtilsWorld(Side.CLIENT);
        ServerUtilsWorld.client.readReloadData(io);
        new EventServerUtilitiesWorldClient(ServerUtilsWorld.client).post();
        if (first && hasServerUtilities && ServerUtilitiesLib.serverUtilitiesIntegration != null)
            ServerUtilitiesLib.serverUtilitiesIntegration.readWorldData(io);

        return null;
    }
}
