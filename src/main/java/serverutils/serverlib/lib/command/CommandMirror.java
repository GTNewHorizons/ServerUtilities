package serverutils.serverlib.lib.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.List;

public class CommandMirror extends CommandBase
{
	public final ICommand mirrored;

	public CommandMirror(ICommand c)
	{
		mirrored = c;
	}

	@Override
	public String getName()
	{
		return mirrored.getCommandName();
	}

	@Override
	public String getUsage(ICommandSender sender)
	{
		return mirrored.getCommandUsage(sender);
	}

	@Override
	public List<String> getAliases()
	{
		return mirrored.getCommandAliases();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
	{
		mirrored.execute(server, sender, args);
	}

	@Override
	public int getRequiredPermissionLevel()
	{
		return mirrored instanceof CommandBase ? ((CommandBase) mirrored).getRequiredPermissionLevel() : 4;
	}

	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender)
	{
		return mirrored.checkPermission(server, sender);
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos)
	{
		return mirrored.getTabCompletions(server, sender, args, pos);
	}

	@Override
	public boolean isUsernameIndex(String[] args, int index)
	{
		return mirrored.isUsernameIndex(args, index);
	}

	@Override
	public int compareTo(ICommand o)
	{
		return getName().compareTo(o.getName());
	}
}