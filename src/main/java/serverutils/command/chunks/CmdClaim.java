package serverutils.command.chunks;

import static serverutils.ServerUtilitiesNotifications.CANT_MODIFY_CHUNK;
import static serverutils.ServerUtilitiesNotifications.CHUNK_MODIFIED;

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

public class CmdClaim extends CmdBase {

    public CmdClaim() {
        super("claim", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (!ClaimedChunks.isActive()) {
            throw ServerUtilities.error(sender, "feature_disabled_server");
        }

        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ForgePlayer p = CommandUtils.getSelfOrOther(player, args, 0, ServerUtilitiesPermissions.CLAIMS_OTHER_CLAIM);
        ChunkDimPos pos = new ChunkDimPos(player);

        if (!player.getUniqueID().equals(p.getId())
                && !ClaimedChunks.instance.canPlayerModify(p, pos, ServerUtilitiesPermissions.CLAIMS_OTHER_CLAIM)) {
            CANT_MODIFY_CHUNK.createNotification("serverutilities.lang.chunks.cant_modify_chunk").setError()
                    .send(player);
            return;
        }

        switch (ClaimedChunks.instance.claimChunk(p, pos)) {
            case SUCCESS:
                CHUNK_MODIFIED.send(player, "serverutilities.lang.chunks.chunk_claimed");
                ServerUtilitiesNotifications.updateChunkMessage(player, pos);
                break;
            case DIMENSION_BLOCKED:
                CANT_MODIFY_CHUNK.createNotification("serverutilities.lang.chunks.claiming_not_enabled_dim").setError()
                        .send(player);
                break;
            case NO_POWER:
                break;
            default:
                CANT_MODIFY_CHUNK.createNotification("serverutilities.lang.chunks.cant_modify_chunk").setError()
                        .send(player);
                break;
        }
    }
}
