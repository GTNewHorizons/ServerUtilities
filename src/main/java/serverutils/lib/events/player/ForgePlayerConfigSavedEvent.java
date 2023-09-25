package serverutils.lib.events.player;

import net.minecraft.command.ICommandSender;

import serverutils.lib.lib.config.ConfigGroup;
import serverutils.lib.lib.data.ForgePlayer;

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
