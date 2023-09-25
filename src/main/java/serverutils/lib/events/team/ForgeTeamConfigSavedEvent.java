package serverutils.lib.events.team;

import net.minecraft.command.ICommandSender;

import serverutils.lib.lib.config.ConfigGroup;
import serverutils.lib.lib.data.ForgeTeam;

public class ForgeTeamConfigSavedEvent extends ForgeTeamEvent {

    private final ConfigGroup config;
    private final ICommandSender sender;

    public ForgeTeamConfigSavedEvent(ForgeTeam team, ConfigGroup s, ICommandSender ics) {
        super(team);
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