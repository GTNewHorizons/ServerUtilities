package serverutils.lib;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import serverutils.lib.lib.data.ForgePlayer;
import serverutils.lib.lib.data.Universe;
import serverutils.lib.lib.util.ServerUtils;
import serverutils.lib.net.MessageSyncData;

public class ServerUtilitiesLibEventHandler {

    public static final ServerUtilitiesLibEventHandler INST = new ServerUtilitiesLibEventHandler();

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player.ticksExisted % 5 == 2 && event.player instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            byte opState = player.getEntityData().getByte("ServerLibOP");
            byte newOpState = ServerUtils.isOP(player) ? (byte) 2 : (byte) 1;

            if (opState != newOpState) {
                player.getEntityData().setByte("ServerLibOP", newOpState);
                Universe.get().clearCache();
                ForgePlayer forgePlayer = Universe.get().getPlayer(player.getGameProfile());
                if (forgePlayer != null) {
                    new MessageSyncData(false, player, forgePlayer).sendTo(player);
                }
            }
        }
    }
}
