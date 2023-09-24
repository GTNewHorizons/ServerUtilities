package serverutils.serverlib.command.team;

import serverutils.serverlib.ServerLib;
import serverutils.serverlib.events.team.ForgeTeamCreatedEvent;
import serverutils.serverlib.lib.EnumTeamColor;
import serverutils.serverlib.lib.command.CmdBase;
import serverutils.serverlib.lib.data.ForgeTeam;
import serverutils.serverlib.lib.data.TeamType;
import serverutils.serverlib.lib.data.Universe;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

public class CmdCreateServerTeam extends CmdBase
{
	public CmdCreateServerTeam()
	{
		super("create_server_team", Level.OP_OR_SP);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
	{
		checkArgs(sender, args, 1);

		if (!CmdCreate.isValidTeamID(args[0]))
		{
			throw ServerLib.error(sender, "ftblib.lang.team.id_invalid");
		}

		if (Universe.get().getTeam(args[0]).isValid())
		{
			throw ServerLib.error(sender, "ftblib.lang.team.id_already_exists");
		}

		Universe universe = Universe.get();
		universe.clearCache();
		ForgeTeam team = new ForgeTeam(universe, universe.generateTeamUID((short) 0), args[0], TeamType.SERVER);
		team.setTitle(team.getId());
		team.setColor(EnumTeamColor.NAME_MAP.getRandom(sender.getEntityWorld().rand));
		team.universe.addTeam(team);
		new ForgeTeamCreatedEvent(team).post();
		sender.addChatMessage(ServerLib.lang(sender, "ftblib.lang.team.created", team.getId()));
		team.markDirty();
	}
}