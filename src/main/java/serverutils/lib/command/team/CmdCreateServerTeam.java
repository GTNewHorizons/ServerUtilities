package serverutils.lib.command.team;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.events.team.ForgeTeamCreatedEvent;
import serverutils.lib.lib.EnumTeamColor;
import serverutils.lib.lib.command.CmdBase;
import serverutils.lib.lib.data.ForgeTeam;
import serverutils.lib.lib.data.TeamType;
import serverutils.lib.lib.data.Universe;

/**
 * @author LatvianModder
 */
public class CmdCreateServerTeam extends CmdBase {

    public CmdCreateServerTeam() {
        super("create_server_team", Level.OP_OR_SP);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        checkArgs(sender, args, 1);

        if (!CmdCreate.isValidTeamID(args[0])) {
            throw ServerUtilitiesLib.error(sender, "serverlib.lang.team.id_invalid");
        }

        if (Universe.get().getTeam(args[0]).isValid()) {
            throw ServerUtilitiesLib.error(sender, "serverlib.lang.team.id_already_exists");
        }

        Universe universe = Universe.get();
        universe.clearCache();
        ForgeTeam team = new ForgeTeam(universe, universe.generateTeamUID((short) 0), args[0], TeamType.SERVER);
        team.setTitle(team.getId());
        team.setColor(EnumTeamColor.NAME_MAP.getRandom(sender.getEntityWorld().rand));
        team.universe.addTeam(team);
        new ForgeTeamCreatedEvent(team).post();
        sender.addChatMessage(ServerUtilitiesLib.lang(sender, "serverlib.lang.team.created", team.getId()));
        team.markDirty();
    }
}
