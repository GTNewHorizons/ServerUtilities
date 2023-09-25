package serverutils.utils.command.tp;

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.lib.lib.command.CmdBase;
import serverutils.lib.lib.data.Universe;
import serverutils.utils.ServerUtilities;
import serverutils.utils.data.ServerUtilitiesUniverseData;

public class CmdDelWarp extends CmdBase {

    public CmdDelWarp() {
        super("delwarp", Level.OP);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsFromIterableMatchingLastWord(args, ServerUtilitiesUniverseData.WARPS.list());
        }

        return super.addTabCompletionOptions(sender, args);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        checkArgs(sender, args, 1);

        args[0] = args[0].toLowerCase();

        if (ServerUtilitiesUniverseData.WARPS.set(args[0], null)) {
            sender.addChatMessage(ServerUtilities.lang(sender, "serverutilities.lang.warps.del", args[0]));
            Universe.get().markDirty();
        } else {
            throw ServerUtilities.error(sender, "serverutilities.lang.warps.not_set", args[0]);
        }
    }
}
