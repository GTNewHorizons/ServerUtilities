package serverutils.lib.mod.config;

import java.io.File;

import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.api.config.ConfigFile;
import serverutils.lib.api.config.ConfigGroup;
import serverutils.lib.api.config.ConfigRegistry;

public class ServerUtilitiesLibraryConfig {

    public static final ConfigFile configFile = new ConfigFile("serverutilslib");

    public static void load() {
        // TODO: Backwards Compat?
        configFile.setFile(new File(ServerUtilitiesLib.folderLocal, "serverutilslib.json"));
        configFile.setDisplayName("ServerUtilitiesLibrary");
        configFile.add(new ConfigGroup("compat").addAll(ServerUtilitiesLibConfigCompat.class, null, false), false);
        configFile.add(new ConfigGroup("commands").addAll(ServerUtilitiesLibConfigCmd.class, null, false), false);
        configFile.add(
                new ConfigGroup("command_names").addAll(ServerUtilitiesLibConfigCmdNames.class, null, false),
                false);
        ConfigRegistry.add(configFile);
        configFile.load();
    }
}
