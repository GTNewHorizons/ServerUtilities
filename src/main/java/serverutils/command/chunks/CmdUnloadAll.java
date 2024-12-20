package serverutils.command.chunks;

import java.util.List;
import java.util.OptionalInt;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesNotifications;
import serverutils.ServerUtilitiesPermissions;
import serverutils.data.ClaimedChunk;
import serverutils.data.ClaimedChunks;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.data.ForgePlayer;

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
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);

        if (p.hasTeam()) {
            OptionalInt dimension = CommandUtils.parseDimension(sender, args, 0);

            for (ClaimedChunk chunk : ClaimedChunks.instance.getTeamChunks(p.team, dimension)) {
                chunk.setLoaded(false);
            }

            ServerUtilitiesNotifications.CHUNK_MODIFIED.send(player, "serverutilities.lang.chunks.unloaded_all");
        } else {
            throw ServerUtilities.error(sender, "serverutilities.lang.team.error.no_team");
        }
    }
}
