package serverutils.utils.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import com.feed_the_beast.ftblib.lib.command.CmdBase;

/**
 * @author LatvianModder
 */
public class CmdCycleBlockState extends CmdBase {

    public CmdCycleBlockState() {
        super("cycle_block_state", Level.OP);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        // EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        // MovingObjectPosition result = MathUtils.rayTrace(player, false);

        // if (result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {

        // Block block = sender.getEntityWorld().getBlock(result.blockX, result.blockY, result.blockZ);
        // int meta = player.worldObj.getBlockMetadata(result.blockX, result.blockY, result.blockZ);

        // List<IBlockState> states = state.getBlock().getBlockState().getValidStates();
        // player.world.setBlockState(result.getBlockPos(), states.get((states.indexOf(state) + 1) % states.size()),
        // 3);

        // TileEntity tileEntity = player.world.getTileEntity(result.getBlockPos());

        // if (tileEntity != null) {
        // tileEntity.updateContainingBlockInfo();
        // tileEntity.markDirty();
        // }
        // }
        sender.addChatMessage(new ChatComponentText("Command not implemented."));
    }
}
