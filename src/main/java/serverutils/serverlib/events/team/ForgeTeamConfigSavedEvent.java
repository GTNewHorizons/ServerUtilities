package serverutils.serverlib.events.team;

import serverutils.serverlib.lib.config.ConfigGroup;
import serverutils.serverlib.lib.data.ForgeTeam;
import net.minecraft.command.ICommandSender;

public class ForgeTeamConfigSavedEvent extends ForgeTeamEvent {
	private final ConfigGroup config;
	private final ICommandSender sender;

	public ForgeTeamConfigSavedEvent(ForgeTeam team, ConfigGroup s, ICommandSender ics) {
		super(team);
		config = s;
		sender = ics;
	}

	public ConfigGroup getConfig()
	{
		return config;
	}

	public ICommandSender getSender()
	{
		return sender;
	}
}