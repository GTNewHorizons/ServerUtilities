package serverutils.command.ranks;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.ServerUtilities;
import serverutils.lib.command.CmdBase;
import serverutils.ranks.Rank;
import serverutils.ranks.Ranks;

public class CmdCreate extends CmdBase {

    public CmdCreate() {
        super("create", Level.OP);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (!Ranks.isActive()) {
            throw ServerUtilities.errorFeatureDisabledServer(sender);
        }

        checkArgs(sender, args, 1);

        if (!Ranks.isValidName(args[0])) {
            throw ServerUtilities.error(sender, "commands.ranks.create.id_invalid", args[0]);
        } else if (Ranks.INSTANCE.getRank(args[0]) != null) {
            throw ServerUtilities.error(sender, "commands.ranks.create.id_exists", args[0]);
        }

        Rank rank = new Rank(Ranks.INSTANCE, args[0]);

        if (args.length > 1) {
            for (int i = 1; i < args.length; i++) {
                rank.addParent(Ranks.INSTANCE.getRank(sender, args[1].toLowerCase()));
            }
        }

        if (rank.add()) {
            rank.ranks.save();
            sender.addChatMessage(ServerUtilities.lang(sender, "commands.ranks.create.added", rank.getDisplayName()));
        }
    }
}
