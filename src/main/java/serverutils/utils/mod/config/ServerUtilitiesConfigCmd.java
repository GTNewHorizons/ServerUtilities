package serverutils.utils.mod.config;

import serverutils.lib.api.config.ConfigEntryBool;
import serverutils.lib.api.config.ConfigEntryString;

public class ServerUtilitiesConfigCmd {

    public static final ConfigEntryString name_admin = new ConfigEntryString("name_admin", "admin");
    public static final ConfigEntryBool back = new ConfigEntryBool("back", true);
    public static final ConfigEntryBool home = new ConfigEntryBool("home", true);
    public static final ConfigEntryBool spawn = new ConfigEntryBool("spawn", true);
    public static final ConfigEntryBool tplast = new ConfigEntryBool("tplast", true);
    public static final ConfigEntryBool warp = new ConfigEntryBool("warp", true);
}
