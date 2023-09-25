package serverutils.utils.command.tp;

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import com.feed_the_beast.ftblib.lib.command.CmdBase;
import com.feed_the_beast.ftblib.lib.command.CommandUtils;
import com.feed_the_beast.ftblib.lib.data.Universe;
import serverutils.utils.ServerUtilities;
import serverutils.utils.data.FTBUtilitiesPlayerData;

public class CmdDelHome extends CmdBase {

    public CmdDelHome() {
        super("delhome", Level.ALL);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsFromIterableMatchingLastWord(
                    args,
                    FTBUtilitiesPlayerData.get(Universe.get().getPlayer(sender)).homes.list());
        }

        return super.addTabCompletionOptions(sender, args);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        FTBUtilitiesPlayerData data = FTBUtilitiesPlayerData.get(CommandUtils.getForgePlayer(sender));

        if (args.length == 0) {
            args = new String[] { "home" };
        }

        args[0] = args[0].toLowerCase();

        if (data.homes.set(args[0], null)) {
            sender.addChatMessage(ServerUtilities.lang(sender, "ftbutilities.lang.homes.del", args[0]));
            data.player.markDirty();
        } else {
            throw ServerUtilities.error(sender, "ftbutilities.lang.homes.not_set", args[0]);
        }
    }
}
