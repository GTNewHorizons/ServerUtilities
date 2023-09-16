package serverutils.utils.mod.config;

import java.io.File;

import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.api.config.*;
import serverutils.utils.world.ranks.Ranks;

public class ServerUtilitiesConfig {

    public static final ConfigFile configFile = new ConfigFile("serverutils");

    public static void load() {
        configFile.setFile(new File(ServerUtilitiesLib.folderLocal, "serverutils/config.json"));
        configFile.setDisplayName("ServerUtilities");
        configFile.addGroup("backups", ServerUtilitiesConfigBackups.class);
        configFile.addGroup("commands", ServerUtilitiesConfigCmd.class);
        configFile.addGroup("general", ServerUtilitiesConfigGeneral.class);
        configFile.addGroup("login", ServerUtilitiesConfigLogin.class);
        configFile.addGroup("tops", ServerUtilitiesConfigTops.class);
        configFile.addGroup("chunkloading", ServerUtilitiesConfigChunkloading.class);
        Ranks.load(configFile);

        ConfigRegistry.add(configFile);
        configFile.load();
    }
}
