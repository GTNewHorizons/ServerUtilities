package serverutils.serverlib.command.team;


import serverutils.serverlib.lib.command.CmdBase;
import serverutils.serverlib.lib.command.CommandUtils;
import serverutils.serverlib.lib.data.ForgePlayer;
import serverutils.serverlib.lib.data.Universe;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import serverutils.serverlib.lib.math.BlockDimPos;

import javax.annotation.Nullable;
import java.util.List;

public class CmdGet extends CmdBase
{
	public CmdGet()
	{
		super("get", Level.ALL);
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockDimPos pos)
	{
		if (args.length == 1)
		{
			return getListOfStringsMatchingLastWord(args, Universe.get().getPlayers());
		}

		return super.getTabCompletions(server, sender, args, pos);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
	{
		checkArgs(sender, args, 1);
		ForgePlayer player = CommandUtils.getSelfOrOther(sender, args, 0);
		IChatComponent component = new ChatComponentText("");
		component.appendSibling(player.getDisplayName());
		component.appendText(": ");
		component.appendSibling(player.team.getCommandTitle());
		sender.addChatMessage(component);
	}
}
