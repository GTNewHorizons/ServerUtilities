package ftb.lib.mod.config;

import ftb.lib.api.cmd.CommandLevel;
import ftb.lib.api.config.ConfigEntryBool;
import ftb.lib.api.config.ConfigEntryEnum;
import latmod.lib.annotations.Info;

public class FTBLibConfigCmd {

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
