package serverutils.utils.command.tp;

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import com.feed_the_beast.ftblib.lib.command.CmdBase;
import com.feed_the_beast.ftblib.lib.command.CommandUtils;
import com.feed_the_beast.ftblib.lib.data.Universe;
import com.feed_the_beast.ftblib.lib.math.BlockDimPos;
import serverutils.utils.ServerUtilities;
import serverutils.utils.data.FTBUtilitiesUniverseData;

public class CmdSetWarp extends CmdBase {

    public CmdSetWarp() {
        super("setwarp", Level.OP);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 5) {
            return getListOfStringsFromIterableMatchingLastWord(args, CommandUtils.getDimensionNames());
        }

        return super.addTabCompletionOptions(sender, args);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        checkArgs(sender, args, 1);
        BlockDimPos pos;

        args[0] = args[0].toLowerCase();

        if (args.length == 2) {
            pos = new BlockDimPos(sender);
        } else if (args.length >= 4) {
            int x = parseInt(sender, args[1]);
            int y = parseInt(sender, args[2]);
            int z = parseInt(sender, args[3]);
            pos = new BlockDimPos(
                    x,
                    y,
                    z,
                    args.length >= 5 ? parseInt(sender, args[4]) : sender.getEntityWorld().provider.dimensionId);
        } else {
            pos = new BlockDimPos(sender);
        }

        FTBUtilitiesUniverseData.WARPS.set(args[0], pos);
        sender.addChatMessage(ServerUtilities.lang(sender, "ftbutilities.lang.warps.set", args[0]));
        Universe.get().markDirty();
    }
}
