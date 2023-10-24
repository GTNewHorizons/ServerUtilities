package serverutils.command.chunks;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesNotifications;
import serverutils.ServerUtilitiesPermissions;
import serverutils.data.ClaimedChunks;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.util.text_components.Notification;

public class CmdLoad extends CmdBase {

    public CmdLoad() {
        super("load", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (!ClaimedChunks.isActive()) {
            throw ServerUtilities.error(sender, "feature_disabled_server");
        }

        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ForgePlayer p = CommandUtils.getForgePlayer(player);
        ChunkDimPos pos = new ChunkDimPos(player);

        if (p.hasTeam() && ClaimedChunks.instance.canPlayerModify(p, pos, ServerUtilitiesPermissions.CLAIMS_OTHER_LOAD)
                && ClaimedChunks.instance.loadChunk(p, p.team, pos)) {
            Notification
                    .of(
                            ServerUtilitiesNotifications.CHUNK_MODIFIED,
                            ServerUtilities.lang(player, "serverutilities.lang.chunks.chunk_loaded"))
                    .send(player.mcServer, player);
            ServerUtilitiesNotifications.updateChunkMessage(player, pos);
        } else {
            ServerUtilitiesNotifications.sendCantModifyChunk(player.mcServer, player);
        }
    }
}
