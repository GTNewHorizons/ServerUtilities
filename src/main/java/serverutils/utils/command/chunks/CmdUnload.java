package serverutils.utils.command.chunks;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import com.feed_the_beast.ftblib.FTBLib;
import com.feed_the_beast.ftblib.lib.command.CmdBase;
import com.feed_the_beast.ftblib.lib.command.CommandUtils;
import com.feed_the_beast.ftblib.lib.data.ForgePlayer;
import com.feed_the_beast.ftblib.lib.math.ChunkDimPos;
import com.feed_the_beast.ftblib.lib.util.text_components.Notification;
import serverutils.utils.ServerUtilities;
import serverutils.utils.ServerUtilitiesNotifications;
import serverutils.utils.ServerUtilitiesPermissions;
import serverutils.utils.data.ClaimedChunks;

/**
 * @author LatvianModder
 */
public class CmdUnload extends CmdBase {

    public CmdUnload() {
        super("unload", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (!ClaimedChunks.isActive()) {
            throw FTBLib.error(sender, "feature_disabled_server");
        }

        EntityPlayerMP player = getCommandSenderAsPlayer(sender);

        ForgePlayer p = CommandUtils.getForgePlayer(player);
        ChunkDimPos pos = new ChunkDimPos(player);

        if (ClaimedChunks.instance.canPlayerModify(p, pos, ServerUtilitiesPermissions.CLAIMS_OTHER_UNLOAD)
                && ClaimedChunks.instance.unloadChunk(p, pos)) {
            Notification
                    .of(
                            ServerUtilitiesNotifications.CHUNK_MODIFIED,
                            ServerUtilities.lang(player, "ftbutilities.lang.chunks.chunk_unloaded"))
                    .send(player.mcServer, player);
            ServerUtilitiesNotifications.updateChunkMessage(player, pos);
        } else {
            ServerUtilitiesNotifications.sendCantModifyChunk(player.mcServer, player);
        }
    }
}
