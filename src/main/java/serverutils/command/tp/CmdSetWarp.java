package serverutils.command.tp;

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.ServerUtilities;
import serverutils.data.ServerUtilitiesUniverseData;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.data.Universe;
import serverutils.lib.math.BlockDimPos;

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
            EntityPlayerMP targetPlayer = CommandUtils.getForgePlayer(sender, args[1]).getCommandPlayer(sender);
            pos = new BlockDimPos(targetPlayer);
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

        ServerUtilitiesUniverseData.WARPS.set(args[0], pos);
        sender.addChatMessage(ServerUtilities.lang(sender, "serverutilities.lang.warps.set", args[0]));
        Universe.get().markDirty();
    }
}
