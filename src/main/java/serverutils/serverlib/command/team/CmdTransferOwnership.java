package serverutils.serverlib.command.team;

import serverutils.serverlib.ServerLib;
import serverutils.serverlib.lib.EnumTeamStatus;
import serverutils.serverlib.lib.command.CmdBase;
import serverutils.serverlib.lib.command.CommandUtils;
import serverutils.serverlib.lib.data.ForgePlayer;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

public class CmdTransferOwnership extends CmdBase
{
	public CmdTransferOwnership()
	{
		super("transfer_ownership", Level.ALL);
	}

	@Override
	public boolean isUsernameIndex(String[] args, int index)
	{
		return index == 0;
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
	{
		ForgePlayer p = CommandUtils.getForgePlayer(getCommandSenderAsPlayer(sender));

		if (!p.hasTeam())
		{
			throw ServerLib.error(sender, "serverlib.lang.team.error.no_team");
		}
		else if (!p.team.isOwner(p))
		{
			throw ServerLib.error(sender, "serverlib.lang.team.error.not_owner");
		}

		checkArgs(sender, args, 1);

		ForgePlayer p1 = CommandUtils.getForgePlayer(sender, args[0]);

		if (!p.team.equalsTeam(p1.team))
		{
			throw ServerLib.error(sender, "serverlib.lang.team.error.not_member", p1.getDisplayName());
		}

		p.team.setStatus(p1, EnumTeamStatus.OWNER);
	}
}
