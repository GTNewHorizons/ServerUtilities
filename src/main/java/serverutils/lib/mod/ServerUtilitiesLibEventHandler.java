package serverutils.lib.mod;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import latmod.lib.util.Phase;
import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.ServerUtilsWorld;
import serverutils.lib.api.ServerTickCallback;
import serverutils.lib.mod.net.MessageReload;
import serverutils.lib.mod.net.MessageSendWorldID;

public class ServerUtilitiesLibEventHandler {

    public static final List<ServerTickCallback> callbacks = new ArrayList<>();
    public static final List<ServerTickCallback> pendingCallbacks = new ArrayList<>();

    @SubscribeEvent
    public void onWorldLoaded(WorldEvent.Load e) {
        if (e.world.provider.dimensionId == 0 && !e.world.isRemote) {
            ServerUtilitiesLib.reload(ServerUtilitiesLib.getServer(), false, false);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.player instanceof EntityPlayerMP) {
            /*
             * //FIXME: This is a workaround if(!loaded) {
             * ServerUtilitiesLibrary.reload(ServerUtilitiesLibrary.getServer(), false, false); loaded = true; }
             */

            final EntityPlayerMP ep = (EntityPlayerMP) e.player;
            if (ServerUtilitiesLib.serverUtilitiesIntegration != null)
                ServerUtilitiesLib.serverUtilitiesIntegration.onPlayerJoined(ep, Phase.PRE);
            new MessageSendWorldID(ServerUtilsWorld.server, ep).sendTo(ep);
            if (ServerUtilitiesLib.serverUtilitiesIntegration != null)
                ServerUtilitiesLib.serverUtilitiesIntegration.onPlayerJoined(ep, Phase.POST);
            new MessageReload(ServerUtilsWorld.server, 1).sendTo(ep);
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent e) {
        if (!e.world.isRemote && e.side == Side.SERVER
                && e.phase == TickEvent.Phase.END
                && e.type == TickEvent.Type.WORLD) {
            if (e.world.provider.dimensionId == 0) {
                if (!pendingCallbacks.isEmpty()) {
                    callbacks.addAll(pendingCallbacks);
                    pendingCallbacks.clear();
                }

                if (!callbacks.isEmpty()) {
                    for (int i = callbacks.size() - 1; i >= 0; i--)
                        if (callbacks.get(i).incAndCheck()) callbacks.remove(i);
                }
            }

            if (ServerUtilitiesLib.serverUtilitiesIntegration != null)
                ServerUtilitiesLib.serverUtilitiesIntegration.onServerTick(e.world);
        }
    }

    @SubscribeEvent
    public void onRightClick(PlayerInteractEvent e) {
        if (ServerUtilitiesLib.serverUtilitiesIntegration != null)
            ServerUtilitiesLib.serverUtilitiesIntegration.onRightClick(e);
    }
}
