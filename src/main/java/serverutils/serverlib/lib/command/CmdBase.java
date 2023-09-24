package serverutils.serverlib.lib.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public abstract class CmdBase extends CommandBase implements ICommandWithParent
{
	public static class Level
	{
		public static final Level ALL = new Level(0, (server, sender, command) -> true);
		public static final Level OP_OR_SP = new Level(2, (server, sender, command) -> server.isSinglePlayer() || sender.canCommandSenderUseCommand(2, command.getCommandName()));
		public static final Level OP = new Level(2, (server, sender, command) -> sender.canCommandSenderUseCommand(2, command.getCommandName()));
		public static final Level STRONG_OP_OR_SP = new Level(4, (server, sender, command) -> server.isSinglePlayer() || sender.canCommandSenderUseCommand(4, command.getCommandName()));
		public static final Level STRONG_OP = new Level(4, (server, sender, command) -> sender.canCommandSenderUseCommand(4, command.getCommandName()));
		public static final Level SERVER = new Level(4, (server, sender, command) -> sender instanceof MinecraftServer);

		public interface PermissionChecker
		{
			boolean checkPermission(MinecraftServer server, ICommandSender sender, ICommand command);
		}

		public final int requiredPermissionLevel;
		public final PermissionChecker permissionChecker;

		public Level(int l, PermissionChecker p)
		{
			requiredPermissionLevel = l;
			permissionChecker = p;
		}
	}

	private final String name;
	public final Level level;
	private ICommand parent;

	public CmdBase(String n, Level l)
	{
		name = n;
		level = l;
	}

	@Override
	public final String getName()
	{
		return name;
	}

	@Override
	public int getRequiredPermissionLevel()
	{
		return level.requiredPermissionLevel;
	}

	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender)
	{
		return level.permissionChecker.checkPermission(server, sender, this);
	}

	public void checkArgs(ICommandSender sender, String[] args, int i) throws CommandException
	{
		if (args.length < i)
		{
			throw new WrongUsageException(getUsage(sender));
		}
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args)
	{
		if (args.length == 0)
		{
			return Collections.emptyList();
		}
		else if (isUsernameIndex(args, args.length - 1))
		{
			return getListOfStringsMatchingLastWord(args, server.getAllUsernames()); //OnlinePlayerNames());
		}

		return super.addTabCompletionOptions(sender, args); //getTabCompletions(server, sender, args, pos);
	}

	@Override
	public ICommand getParent()
	{
		return parent;
	}

	@Override
	public void setParent(@Nullable ICommand c)
	{
		parent = c;
	}
}