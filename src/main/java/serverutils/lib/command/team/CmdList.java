package serverutils.lib.command.team;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.lib.lib.command.CmdBase;
import serverutils.lib.lib.data.ForgeTeam;
import serverutils.lib.lib.data.Universe;
import serverutils.lib.ServerUtilitiesLib;

/**
 * @author LatvianModder
 */
public class CmdList extends CmdBase {

	public CmdList() {
		super("list", Level.ALL);
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) throws CommandException {
		sender.addChatMessage(ServerUtilitiesLib.lang(sender, "commands.team.list.teams", Universe.get().getTeams().size()));

		for (ForgeTeam team : Universe.get().getTeams()) {
			sender.addChatMessage(team.getCommandTitle());
		}
	}
}
