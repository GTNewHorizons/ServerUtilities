package serverutils.lib;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ChatComponentTranslation;

import serverutils.lib.lib.util.text_components.Notification;

public class ServerUtilitiesLibNotifications {
	public static final ResourceLocation RELOAD_SERVER = new ResourceLocation(ServerUtilitiesLib.MOD_ID, "reload_server");
	public static final Notification NO_TEAM = Notification.of(
			new ResourceLocation(ServerUtilitiesLib.MOD_ID, "no_team"),
			new ChatComponentTranslation("ftblib.lang.team.error.no_team")).setError();
}