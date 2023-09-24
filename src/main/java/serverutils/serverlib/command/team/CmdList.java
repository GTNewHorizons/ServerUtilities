package serverutils.serverlib.command.team;

import serverutils.serverlib.ServerLib;
import serverutils.serverlib.lib.command.CmdBase;
import serverutils.serverlib.lib.data.ForgeTeam;
import serverutils.serverlib.lib.data.Universe;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

/**
 * @author LatvianModder
 */
public class CmdList extends CmdBase
{
	public CmdList()
	{
		super("list", Level.ALL);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
	{
		sender.addChatMessage(ServerLib.lang(sender, "commands.team.list.teams", Universe.get().getTeams().size()));

		for (ForgeTeam team : Universe.get().getTeams())
		{
			sender.addChatMessage(team.getCommandTitle());
		}
	}
}