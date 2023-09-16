package ftb.lib.mod;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import ftb.lib.FTBLib;
import ftb.lib.FTBWorld;
import ftb.lib.api.ServerTickCallback;
import ftb.lib.mod.net.MessageReload;
import ftb.lib.mod.net.MessageSendWorldID;
import latmod.lib.util.Phase;

public class FTBLibEventHandler {

    public static final List<ServerTickCallback> callbacks = new ArrayList<>();
    public static final List<ServerTickCallback> pendingCallbacks = new ArrayList<>();

    @SubscribeEvent
    public void onWorldLoaded(WorldEvent.Load e) {
        if (e.world.provider.dimensionId == 0 && !e.world.isRemote) {
            FTBLib.reload(FTBLib.getServer(), false, false);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.player instanceof EntityPlayerMP) {
            /*
             * //FIXME: This is a workaround if(!loaded) { FTBLib.reload(FTBLib.getServer(), false, false); loaded =
             * true; }
             */

            final EntityPlayerMP ep = (EntityPlayerMP) e.player;
            if (FTBLib.ftbu != null) FTBLib.ftbu.onPlayerJoined(ep, Phase.PRE);
            new MessageSendWorldID(FTBWorld.server, ep).sendTo(ep);
            if (FTBLib.ftbu != null) FTBLib.ftbu.onPlayerJoined(ep, Phase.POST);
            new MessageReload(FTBWorld.server, 1).sendTo(ep);
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

            if (FTBLib.ftbu != null) FTBLib.ftbu.onServerTick(e.world);
        }
    }

    @SubscribeEvent
    public void onRightClick(PlayerInteractEvent e) {
        if (FTBLib.ftbu != null) FTBLib.ftbu.onRightClick(e);
    }
}
