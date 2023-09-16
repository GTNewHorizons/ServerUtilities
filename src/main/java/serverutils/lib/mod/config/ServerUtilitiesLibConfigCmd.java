package serverutils.lib.mod.config;

import latmod.lib.annotations.Info;
import serverutils.lib.api.cmd.CommandLevel;
import serverutils.lib.api.config.ConfigEntryBool;
import serverutils.lib.api.config.ConfigEntryEnum;

public class ServerUtilitiesLibConfigCmd {

    @Info("A new layout for /list command")
    public static final ConfigEntryBool override_list = new ConfigEntryBool("override_list", true);

    @Info("Can fix some /help problems")
    public static final ConfigEntryBool override_help = new ConfigEntryBool("override_help", true);

    public static final ConfigEntryEnum<CommandLevel> level_set_item_name = new ConfigEntryEnum<>(
            "set_item_name",
            CommandLevel.VALUES,
            CommandLevel.OP,
            false);
    public static final ConfigEntryEnum<CommandLevel> level_trash_can = new ConfigEntryEnum<>(
            "trash_can",
            CommandLevel.VALUES,
            CommandLevel.ALL,
            false);
}
