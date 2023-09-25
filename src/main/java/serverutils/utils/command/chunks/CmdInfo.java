package serverutils.utils.command.chunks;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import com.feed_the_beast.ftblib.FTBLib;
import com.feed_the_beast.ftblib.lib.command.CmdBase;
import com.feed_the_beast.ftblib.lib.math.ChunkDimPos;
import com.feed_the_beast.ftblib.lib.util.ServerUtils;
import serverutils.utils.ServerUtilities;
import serverutils.utils.data.ClaimedChunk;
import serverutils.utils.data.ClaimedChunks;

/**
 * @author LatvianModder
 */
public class CmdInfo extends CmdBase {

    public CmdInfo() {
        super("info", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (!ClaimedChunks.isActive()) {
            throw FTBLib.error(sender, "feature_disabled_server");
        }

        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ChunkDimPos pos = new ChunkDimPos(player);
        ClaimedChunk chunk = ClaimedChunks.instance.getChunk(pos);

        IChatComponent owner;

        if (chunk == null) {
            owner = ServerUtilities.lang(sender, "commands.chunks.info.not_claimed");
        } else {
            owner = chunk.getTeam().getCommandTitle();
        }

        owner.getChatStyle().setColor(EnumChatFormatting.GOLD);
        sender.addChatMessage(
                ServerUtilities.lang(
                        sender,
                        "commands.chunks.info.text",
                        pos.posX,
                        pos.posZ,
                        ServerUtils.getDimensionName(pos.dim),
                        owner));
    }
}
