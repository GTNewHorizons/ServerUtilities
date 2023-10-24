package serverutils.command.team;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.ServerUtilities;
import serverutils.lib.command.CmdBase;
import serverutils.lib.data.ForgeTeam;
import serverutils.lib.data.Universe;

public class CmdList extends CmdBase {

    public CmdList() {
        super("list", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        sender.addChatMessage(
                ServerUtilities.lang(sender, "commands.team.list.teams", Universe.get().getTeams().size()));

        for (ForgeTeam team : Universe.get().getTeams()) {
            sender.addChatMessage(team.getCommandTitle());
        }
    }
}
