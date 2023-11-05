package serverutils.command.chunks;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import serverutils.ServerUtilities;
import serverutils.data.ClaimedChunk;
import serverutils.data.ClaimedChunks;
import serverutils.lib.command.CmdBase;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.util.ServerUtils;

public class CmdInfo extends CmdBase {

    public CmdInfo() {
        super("info", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (!ClaimedChunks.isActive()) {
            throw ServerUtilities.error(sender, "feature_disabled_server");
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
