package serverutils.lib.mod.net;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;

import cpw.mods.fml.common.network.simpleimpl.*;
import latmod.lib.ByteCount;
import latmod.lib.ByteIOStream;
import latmod.lib.LMUtils;
import serverutils.lib.LMNBTUtils;
import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.ServerUtilsWorld;
import serverutils.lib.api.EventServerUtilitiesReload;
import serverutils.lib.api.GameModes;
import serverutils.lib.api.config.ConfigGroup;
import serverutils.lib.api.config.ConfigRegistry;
import serverutils.lib.api.net.LMNetworkWrapper;
import serverutils.lib.api.net.MessageLM;
import serverutils.lib.api.notification.ClientNotifications;
import serverutils.lib.api.notification.Notification;
import serverutils.lib.mod.ServerUtilitiesLibraryMod;
import serverutils.lib.mod.client.ServerUtilitiesLibraryModClient;

public class MessageReload extends MessageLM {

    public MessageReload() {
        super(ByteCount.INT);
    }

    public MessageReload(ServerUtilsWorld w, int reloadClient) {
        this();
        io.writeByte(reloadClient);

        if (reloadClient > 0) {
            w.writeReloadData(io);
            writeSyncedConfig(io);
        }
    }

    public LMNetworkWrapper getWrapper() {
        return ServerUtilitiesLibraryLibNetHandler.NET;
    }

    public IMessage onMessage(MessageContext ctx) {
        if (ServerUtilitiesLib.DEV_ENV) {
            ServerUtilitiesLib.dev_logger.info("--------< RELOADING >----------");
        }

        byte reload = io.readByte();

        if (reload == 0) {
            Notification n = new Notification(
                    "reload_client_config",
                    ServerUtilitiesLibraryMod.mod.chatComponent("reload_client_config"),
                    7000);
            n.title.getChatStyle().setColor(EnumChatFormatting.WHITE);
            n.desc = new ChatComponentText("/" + ServerUtilitiesLibraryModClient.reload_client_cmd.getAsString());
            n.setColor(0xFF333333);
            ClientNotifications.add(n);
            return null;
        }

        long ms = LMUtils.millis();
        ServerUtilsWorld.client.readReloadData(io);
        readSyncedConfig(io);

        if (reload > 0) {
            reloadClient(ms, reload > 1);
        }

        return null;
    }

    public static void reloadClient(long ms, boolean printMessage) {
        if (ms == 0L) ms = LMUtils.millis();
        GameModes.reload();
        EntityPlayer ep = ServerUtilitiesLibraryMod.proxy.getClientPlayer();
        EventServerUtilitiesReload event = new EventServerUtilitiesReload(ServerUtilsWorld.client, ep, true);
        if (ServerUtilitiesLib.serverUtilitiesIntegration != null)
            ServerUtilitiesLib.serverUtilitiesIntegration.onReloaded(event);
        event.post();

        if (printMessage) {
            ServerUtilitiesLib.printChat(
                    ep,
                    ServerUtilitiesLibraryMod.mod.chatComponent("reloaded_client", ((LMUtils.millis() - ms) + "ms")));
        }

        ServerUtilitiesLibraryMod.logger.info("Current Mode: " + ServerUtilsWorld.client.getMode());
    }

    static void writeSyncedConfig(ByteIOStream out) {
        NBTTagCompound tag = new NBTTagCompound();
        ConfigRegistry.synced.writeToNBT(tag, false);
        LMNBTUtils.writeTag(out, tag);

        if (ServerUtilitiesLib.DEV_ENV)
            ServerUtilitiesLib.dev_logger.info("Synced config TX: " + ConfigRegistry.synced.getSerializableElement());
    }

    static void readSyncedConfig(ByteIOStream in) {
        NBTTagCompound tag = LMNBTUtils.readTag(in);
        ConfigGroup synced = new ConfigGroup(ConfigRegistry.synced.getID());
        synced.readFromNBT(tag, false);
        ConfigRegistry.synced.loadFromGroup(synced, true);
        if (ServerUtilitiesLib.DEV_ENV)
            ServerUtilitiesLib.dev_logger.info("Synced config RX: " + synced.getSerializableElement());
    }
}
