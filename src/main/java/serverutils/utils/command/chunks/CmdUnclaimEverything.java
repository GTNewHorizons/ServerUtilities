package serverutils.utils.command.chunks;

import java.util.List;
import java.util.OptionalInt;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.lib.command.CmdBase;
import serverutils.lib.lib.command.CommandUtils;
import serverutils.lib.lib.data.ForgeTeam;
import serverutils.lib.lib.data.Universe;
import serverutils.utils.data.ClaimedChunks;

public class CmdUnclaimEverything extends CmdBase {

    public CmdUnclaimEverything() {
        super("unclaim_everything", Level.OP_OR_SP);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsFromIterableMatchingLastWord(args, CommandUtils.getDimensionNames());
        }

        return super.addTabCompletionOptions(sender, args);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (!ClaimedChunks.isActive()) {
            throw ServerUtilitiesLib.error(sender, "feature_disabled_server");
        }

        OptionalInt dimension = CommandUtils.parseDimension(sender, args, 0);

        for (ForgeTeam team : Universe.get().getTeams()) {
            ClaimedChunks.instance.unclaimAllChunks(null, team, dimension);
        }
    }
}
