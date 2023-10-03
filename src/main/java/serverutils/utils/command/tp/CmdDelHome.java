package serverutils.utils.command.tp;

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.lib.lib.command.CmdBase;
import serverutils.lib.lib.command.CommandUtils;
import serverutils.lib.lib.data.Universe;
import serverutils.mod.ServerUtilities;
import serverutils.utils.data.ServerUtilitiesPlayerData;

public class CmdDelHome extends CmdBase {

    public CmdDelHome() {
        super("delhome", Level.ALL);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsFromIterableMatchingLastWord(
                    args,
                    ServerUtilitiesPlayerData.get(Universe.get().getPlayer(sender)).homes.list());
        }

        return super.addTabCompletionOptions(sender, args);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(CommandUtils.getForgePlayer(sender));

        if (args.length == 0) {
            args = new String[] { "home" };
        }

        args[0] = args[0].toLowerCase();

        if (data.homes.set(args[0], null)) {
            sender.addChatMessage(ServerUtilities.lang(sender, "serverutilities.lang.homes.del", args[0]));
            data.player.markDirty();
        } else {
            throw ServerUtilities.error(sender, "serverutilities.lang.homes.not_set", args[0]);
        }
    }
}
