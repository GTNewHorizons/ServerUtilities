package serverutils.serverlib.lib.command;

import net.minecraft.command.CommandHelp;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.command.ForgeCommand; //.command.CommandTreeBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CmdTreeHelp extends CommandHelp
{
	private final CommandTreeBase parent;

	public CmdTreeHelp(CommandTreeBase parent)
	{
		this.parent = parent;
	}

	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender)
	{
		return true;
	}

	@Override
	protected List<ICommand> getSortedPossibleCommands(ICommandSender sender, MinecraftServer server)
	{
		List<ICommand> list = new ArrayList<>();

		for (ICommand command : parent.getSubCommands())
		{
			if (command.canCommandSenderUseCommand(sender))
			{
				list.add(command);
			}
		}

		Collections.sort(list);
		return list;
	}

	@Override
	protected Map<String, ICommand> getCommandMap(MinecraftServer server)
	{
		return parent.getCommandMap();
	}
}