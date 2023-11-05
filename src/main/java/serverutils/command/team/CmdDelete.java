package serverutils.command.team;

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.ServerUtilities;
import serverutils.lib.command.CmdBase;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.ForgeTeam;
import serverutils.lib.data.Universe;

public class CmdDelete extends CmdBase {

    public CmdDelete() {
        super("delete", Level.OP_OR_SP);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return matchFromIterable(args, Universe.get().getTeams());
        }

        return super.addTabCompletionOptions(sender, args);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        checkArgs(sender, args, 1);

        ForgeTeam team = Universe.get().getTeam(args[0]);

        if (!team.isValid()) {
            throw ServerUtilities.error(sender, "serverutilities.lang.team.error.not_found", args[0]);
        }

        ForgePlayer o = team.getOwner();

        for (ForgePlayer player : team.getMembers()) {
            if (player != o) {
                team.removeMember(player);
            }
        }

        if (o != null) {
            team.removeMember(o);
        }

        team.delete();
    }
}
