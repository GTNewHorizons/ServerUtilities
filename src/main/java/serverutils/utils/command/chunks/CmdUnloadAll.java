package serverutils.utils.command.chunks;

import java.util.List;
import java.util.OptionalInt;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import com.feed_the_beast.ftblib.FTBLib;
import com.feed_the_beast.ftblib.lib.command.CmdBase;
import com.feed_the_beast.ftblib.lib.command.CommandUtils;
import com.feed_the_beast.ftblib.lib.data.ForgePlayer;
import com.feed_the_beast.ftblib.lib.util.text_components.Notification;
import serverutils.utils.ServerUtilities;
import serverutils.utils.ServerUtilitiesNotifications;
import serverutils.utils.ServerUtilitiesPermissions;
import serverutils.utils.data.ClaimedChunk;
import serverutils.utils.data.ClaimedChunks;

/**
 * @author LatvianModder
 */
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
            throw FTBLib.error(sender, "feature_disabled_server");
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
                            ServerUtilities.lang(sender, "ftbutilities.lang.chunks.unloaded_all"))
                    .send(getCommandSenderAsPlayer(sender).mcServer, sender);
        } else {
            throw FTBLib.error(sender, "ftblib.lang.team.error.no_team");
        }
    }
}
