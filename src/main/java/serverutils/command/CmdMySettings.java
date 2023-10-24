package serverutils.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.lib.command.CmdEditConfigBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.config.ConfigGroup;
import serverutils.lib.config.IConfigCallback;

public class CmdMySettings extends CmdEditConfigBase {

    public CmdMySettings() {
        super("my_settings", Level.ALL);
    }

    @Override
    public ConfigGroup getGroup(ICommandSender sender) throws CommandException {
        return CommandUtils.getForgePlayer(sender).getSettings();
    }

    @Override
    public IConfigCallback getCallback(ICommandSender sender) throws CommandException {
        return CommandUtils.getForgePlayer(sender);
    }
}
