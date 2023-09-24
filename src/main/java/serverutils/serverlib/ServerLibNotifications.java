package serverutils.serverlib;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ChatComponentTranslation;

import serverutils.serverlib.lib.util.text_components.Notification;

public class ServerLibNotifications {
	public static final ResourceLocation RELOAD_SERVER = new ResourceLocation(ServerLib.MOD_ID, "reload_server");
	public static final Notification NO_TEAM = Notification.of(
			new ResourceLocation(ServerLib.MOD_ID, "no_team"),
			new ChatComponentTranslation("ftblib.lang.team.error.no_team")).setError();
}