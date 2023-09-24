package serverutils.serverlib;

import serverutils.serverlib.ServerLib;
import serverutils.serverlib.lib.data.Universe;
import serverutils.serverlib.lib.util.ServerUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mod.EventBusSubscriber(modid = ServerLib.MOD_ID)
public class ServerLibEventHandler {
	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event)
	{
		if (event.player.ticksExisted % 5 == 2 && event.player instanceof EntityPlayerMP)
		{
			byte opState = event.player.getEntityData().getByte("FTBLibOP");
			byte newOpState = ServerUtils.isOP((EntityPlayerMP) event.player) ? (byte) 2 : (byte) 1;

			if (opState != newOpState)
			{
				event.player.getEntityData().setByte("FTBLibOP", newOpState);
				Universe.get().clearCache();
			}
		}
	}
}