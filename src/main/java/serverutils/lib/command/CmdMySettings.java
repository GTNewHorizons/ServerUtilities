package serverutils.lib.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.lib.lib.command.CommandUtils;
import serverutils.lib.lib.config.ConfigGroup;
import serverutils.lib.lib.config.IConfigCallback;
import serverutils.lib.lib.command.CmdEditConfigBase;

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