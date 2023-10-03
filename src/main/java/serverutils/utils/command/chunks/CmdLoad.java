package serverutils.utils.command.chunks;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.lib.lib.command.CmdBase;
import serverutils.lib.lib.command.CommandUtils;
import serverutils.lib.lib.data.ForgePlayer;
import serverutils.lib.lib.math.ChunkDimPos;
import serverutils.lib.lib.util.text_components.Notification;
import serverutils.mod.ServerUtilities;
import serverutils.utils.ServerUtilitiesNotifications;
import serverutils.utils.ServerUtilitiesPermissions;
import serverutils.utils.data.ClaimedChunks;

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
