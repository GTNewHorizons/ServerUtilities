package serverutils.utils.mod.config;

import serverutils.lib.api.config.ConfigEntryBool;

public class ServerUtilitiesConfigTops // Top
{

    public static final ConfigEntryBool first_joined = new ConfigEntryBool("first_joined", true);
    public static final ConfigEntryBool last_seen = new ConfigEntryBool("last_seen", true);
    public static final ConfigEntryBool time_played = new ConfigEntryBool("time_played", true);
    public static final ConfigEntryBool deaths = new ConfigEntryBool("deaths", true);
    public static final ConfigEntryBool deaths_ph = new ConfigEntryBool("deaths_ph", true);
}
