package serverutils.command.ranks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.ServerUtilities;
import serverutils.lib.command.CmdBase;
import serverutils.ranks.Rank;
import serverutils.ranks.Ranks;

public class CmdRemove extends CmdBase {

    public CmdRemove() {
        super("remove", Level.OP);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);

        if ((args.length == 1 || args.length == 2) && Ranks.isActive()) {
            List<String> list = new ArrayList<>();

            if (args.length == 1) {
                list.addAll(Arrays.asList(player.mcServer.getConfigurationManager().getAllUsernames()));
            }

            list.addAll(Ranks.INSTANCE.getRankNames(false));
            return getListOfStringsFromIterableMatchingLastWord(args, list);
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
            throw ServerUtilities.errorFeatureDisabledServer(sender);
        }

        checkArgs(sender, args, 1);
        Rank rank = Ranks.INSTANCE.getRank(sender, args[0]);

        if (args.length == 1) {
            if (rank.clearParents()) {
                rank.ranks.save();
                sender.addChatMessage(
                        ServerUtilities.lang(sender, "commands.ranks.remove.text", "*", rank.getDisplayName()));
            }
        } else {
            Rank parent = Ranks.INSTANCE.getRank(sender, args[1]);

            if (rank.removeParent(parent)) {
                rank.ranks.save();
                sender.addChatMessage(
                        ServerUtilities.lang(
                                sender,
                                "commands.ranks.remove.text",
                                parent.getDisplayName(),
                                rank.getDisplayName()));
            }
        }
    }
}
