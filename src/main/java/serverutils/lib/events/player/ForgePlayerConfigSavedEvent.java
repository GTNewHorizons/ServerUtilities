package serverutils.lib.events.player;

import serverutils.lib.lib.config.ConfigGroup;
import serverutils.lib.lib.data.ForgePlayer;
import net.minecraft.command.ICommandSender;

public class ForgePlayerConfigSavedEvent extends ForgePlayerEvent {

	private final ConfigGroup config;
	private final ICommandSender sender;

	public ForgePlayerConfigSavedEvent(ForgePlayer player, ConfigGroup s, ICommandSender ics) {
		super(player);
		config = s;
		sender = ics;
	}

	public ConfigGroup getConfig() {
		return config;
	}

	public ICommandSender getSender() {
		return sender;
	}
}