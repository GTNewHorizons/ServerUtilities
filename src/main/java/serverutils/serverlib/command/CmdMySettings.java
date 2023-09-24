package serverutils.serverlib.command;

import serverutils.serverlib.lib.command.CmdEditConfigBase;
import serverutils.serverlib.lib.command.CommandUtils;
import serverutils.serverlib.lib.config.ConfigGroup;
import serverutils.serverlib.lib.config.IConfigCallback;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

public class CmdMySettings extends CmdEditConfigBase
{
	public CmdMySettings()
	{
		super("my_settings", Level.ALL);
	}

	@Override
	public ConfigGroup getGroup(ICommandSender sender) throws CommandException
	{
		return CommandUtils.getForgePlayer(sender).getSettings();
	}

	@Override
	public IConfigCallback getCallback(ICommandSender sender) throws CommandException
	{
		return CommandUtils.getForgePlayer(sender);
	}
}