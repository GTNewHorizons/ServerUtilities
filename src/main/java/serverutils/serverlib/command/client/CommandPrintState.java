package serverutils.serverlib.command.client;

import serverutils.serverlib.lib.command.CmdBase;
import serverutils.serverlib.lib.math.BlockDimPos;
import serverutils.serverlib.lib.util.BlockUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.event.ClickEvent;

public class CommandPrintState extends CmdBase
{
	public CommandPrintState()
	{
		super("print_block_state", Level.ALL);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, final String[] args) throws CommandException
	{
		RayTraceResult ray = Minecraft.getMinecraft().objectMouseOver;
		if (ray.typeOfHit != RayTraceResult.Type.BLOCK)
		{
			return;
		}

		BlockDimPos pos = ray.getBlockPos();
		IBlockState state = sender.getEntityWorld().getBlockState(pos);

		IChatComponent component = new ChatComponentText(state.getBlock().getPickBlock(state, ray, sender.getEntityWorld(), pos, Minecraft.getMinecraft().thePlayer).getDisplayName() + " :: " + BlockUtils.getNameFromState(state));
		component.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, BlockUtils.getNameFromState(state)));
		sender.addChatMessage(component);
	}
}