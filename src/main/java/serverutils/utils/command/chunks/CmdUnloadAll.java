package serverutils.utils.command.chunks;

import java.util.List;
import java.util.OptionalInt;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.lib.lib.command.CmdBase;
import serverutils.lib.lib.command.CommandUtils;
import serverutils.lib.lib.data.ForgePlayer;
import serverutils.lib.lib.util.text_components.Notification;
import serverutils.mod.ServerUtilities;
import serverutils.mod.ServerUtilitiesNotifications;
import serverutils.utils.ServerUtilitiesPermissions;
import serverutils.utils.data.ClaimedChunk;
import serverutils.utils.data.ClaimedChunks;

public class CmdUnloadAll extends CmdBase {

    public CmdUnloadAll() {
        super("unload_all", Level.ALL);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsFromIterableMatchingLastWord(args, CommandUtils.getDimensionNames());
        }

        return super.addTabCompletionOptions(sender, args);
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 1;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (!ClaimedChunks.isActive()) {
            throw ServerUtilities.error(sender, "feature_disabled_server");
        }

        ForgePlayer p = CommandUtils.getSelfOrOther(sender, args, 1, ServerUtilitiesPermissions.CLAIMS_OTHER_UNLOAD);

        if (p.hasTeam()) {
            OptionalInt dimension = CommandUtils.parseDimension(sender, args, 0);

            for (ClaimedChunk chunk : ClaimedChunks.instance.getTeamChunks(p.team, dimension)) {
                chunk.setLoaded(false);
            }

            Notification
                    .of(
                            ServerUtilitiesNotifications.UNCLAIMED_ALL,
                            ServerUtilities.lang(sender, "serverutilities.lang.chunks.unloaded_all"))
                    .send(getCommandSenderAsPlayer(sender).mcServer, sender);
        } else {
            throw ServerUtilities.error(sender, "serverutilities.lang.team.error.no_team");
        }
    }
}
