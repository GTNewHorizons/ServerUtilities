package serverutils.serverlib.command.team;

import serverutils.serverlib.FTBLib;
import serverutils.serverlib.ServerLib;
import serverutils.serverlib.lib.command.CmdBase;
import serverutils.serverlib.lib.data.ForgePlayer;
import serverutils.serverlib.lib.data.ForgeTeam;
import serverutils.serverlib.lib.data.Universe;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import serverutils.serverlib.lib.math.BlockDimPos;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author LatvianModder
 */
public class CmdDelete extends CmdBase
{
	public CmdDelete()
	{
		super("delete", Level.OP_OR_SP);
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockDimPos pos)
	{
		if (args.length == 1)
		{
			return getListOfStringsMatchingLastWord(args, Universe.get().getTeams());
		}

		return super.getTabCompletions(server, sender, args, pos);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
	{
		checkArgs(sender, args, 1);

		ForgeTeam team = Universe.get().getTeam(args[0]);

		if (!team.isValid())
		{
			throw ServerLib.error(sender, "serverlib.lang.team.error.not_found", args[0]);
		}

		ForgePlayer o = team.getOwner();

		for (ForgePlayer player : team.getMembers())
		{
			if (player != o)
			{
				team.removeMember(player);
			}
		}

		if (o != null)
		{
			team.removeMember(o);
		}

		team.delete();
	}
}