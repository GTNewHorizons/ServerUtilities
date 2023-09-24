package serverutils.serverlib;

import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.serverlib.lib.data.ForgePlayer;
import serverutils.serverlib.lib.data.Universe;
import serverutils.serverlib.lib.util.ServerUtils;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import serverutils.serverlib.net.MessageSyncData;

public class ServerLibEventHandler {

	public static final ServerLibEventHandler INST = new ServerLibEventHandler();

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