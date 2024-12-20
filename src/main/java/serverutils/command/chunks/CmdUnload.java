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

public class CmdUnload extends CmdBase {

    public CmdUnload() {
        super("unload", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (!ClaimedChunks.isActive()) {
            throw ServerUtilities.error(sender, "feature_disabled_server");
        }

        EntityPlayerMP player = getCommandSenderAsPlayer(sender);

        ForgePlayer p = CommandUtils.getForgePlayer(player);
        ChunkDimPos pos = new ChunkDimPos(player);

        if (ClaimedChunks.instance.canPlayerModify(p, pos, ServerUtilitiesPermissions.CLAIMS_OTHER_UNLOAD)
                && ClaimedChunks.instance.unloadChunk(p, pos)) {
            CHUNK_MODIFIED.send(player, "serverutilities.lang.chunks.chunk_unloaded");
            ServerUtilitiesNotifications.updateChunkMessage(player, pos);
        } else {
            CANT_MODIFY_CHUNK.createNotification("serverutilities.lang.chunks.cant_modify_chunk").setError()
                    .send(player);
        }
    }
}
