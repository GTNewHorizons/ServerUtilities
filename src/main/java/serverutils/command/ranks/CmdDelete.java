package serverutils.command.ranks;

import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.ServerUtilities;
import serverutils.lib.command.CmdBase;
import serverutils.ranks.Rank;
import serverutils.ranks.Ranks;

public class CmdDelete extends CmdBase {

    public CmdDelete() {
        super("delete", Level.OP);
    }

    @Override
    public List<String> getCommandAliases() {
        return Collections.singletonList("del");
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1 && Ranks.isActive()) {
            return getListOfStringsFromIterableMatchingLastWord(args, Ranks.INSTANCE.getRankNames(false));
        }

        return super.addTabCompletionOptions(sender, args);
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (!Ranks.isActive()) {
            throw ServerUtilities.error(sender, "feature_disabled_server");
        }

        checkArgs(sender, args, 1);

        Rank rank = Ranks.INSTANCE.getRank(sender, args[0]);

        if (rank.remove()) {
            rank.ranks.save();
            sender.addChatMessage(ServerUtilities.lang(sender, "commands.ranks.delete.deleted", rank.getDisplayName()));
        } else {
            sender.addChatMessage(ServerUtilities.lang(sender, "nothing_changed"));
        }
    }
}
