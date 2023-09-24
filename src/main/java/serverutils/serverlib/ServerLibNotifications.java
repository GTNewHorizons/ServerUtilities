package serverutils.serverlib;

import serverutils.serverlib.lib.util.text_components.Notification;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ChatComponentTranslation;

public class ServerLibNotifications
{
	public static final ResourceLocation RELOAD_SERVER = new ResourceLocation(ServerLib.MOD_ID, "reload_server");
	public static final Notification NO_TEAM = Notification.of(new ResourceLocation(ServerLib.MOD_ID, "no_team"), new ChatComponentTranslation("ftblib.lang.team.error.no_team")).setError();
}