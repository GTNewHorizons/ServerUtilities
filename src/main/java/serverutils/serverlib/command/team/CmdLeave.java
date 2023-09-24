package serverutils.serverlib.command.team;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.serverlib.ServerLib;
import serverutils.serverlib.lib.command.CmdBase;
import serverutils.serverlib.lib.command.CommandUtils;
import serverutils.serverlib.lib.data.ForgePlayer;

public class CmdLeave extends CmdBase {
	public CmdLeave() {
		super("leave", Level.ALL);
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) throws CommandException {
		ForgePlayer p = CommandUtils.getForgePlayer(getCommandSenderAsPlayer(sender));

		if (!p.hasTeam()) {
			throw ServerLib.error(sender, "serverlib.lang.team.error.no_team");
		} else if (!p.team.removeMember(p)) {
			throw ServerLib.error(sender, "serverlib.lang.team.error.must_transfer_ownership");
		}
	}
}