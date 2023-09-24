package serverutils.serverlib.lib.command;

import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;

import javax.annotation.Nullable;

/**
 * @author LatvianModder
 */
public interface ICommandWithParent extends ICommand
{
	@Nullable
	ICommand getParent();

	void setParent(@Nullable ICommand p);

	@Override
	default String getCommandUsage(ICommandSender sender)
	{
		return "commands." + getCommandPath(this) + ".usage";
	}

	static String getCommandPath(ICommand command)
	{
		return (command instanceof ICommandWithParent && ((ICommandWithParent) command).getParent() != null ? (getCommandPath(((ICommandWithParent) command).getParent()) + ".") : "") + command.getCommandName();
	}
}