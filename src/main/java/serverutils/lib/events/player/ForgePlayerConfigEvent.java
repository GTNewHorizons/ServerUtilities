package serverutils.lib.events.player;

import serverutils.lib.lib.config.ConfigGroup;
import serverutils.lib.lib.data.ForgePlayer;

public class ForgePlayerConfigEvent extends ForgePlayerEvent {

	private final ConfigGroup config;

	public ForgePlayerConfigEvent(ForgePlayer player, ConfigGroup s) {
		super(player);
		config = s;
	}

	public ConfigGroup getConfig() {
		return config;
	}
}