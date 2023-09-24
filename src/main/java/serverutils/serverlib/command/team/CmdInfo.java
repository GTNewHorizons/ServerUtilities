package serverutils.serverlib.command.team;

import serverutils.serverlib.ServerLib;
import serverutils.serverlib.lib.command.CmdBase;
import serverutils.serverlib.lib.data.ForgePlayer;
import serverutils.serverlib.lib.data.ForgeTeam;
import serverutils.serverlib.lib.data.Universe;
import serverutils.serverlib.lib.util.StringUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author LatvianModder
 */
public class CmdInfo extends CmdBase
{
	public CmdInfo()
	{
		super("info", Level.ALL);
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos)
	{
		if (args.length == 1)
		{
			return getListOfStringsMatchingLastWord(args, Universe.get().getTeams());
		}

		return super.getTabCompletions(server, sender, args, pos);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
	{
		checkArgs(sender, args, 1);
		ForgeTeam team = Universe.get().getTeam(args[0]);

		if (!team.isValid())
		{
			throw FTBLib.error(sender, "ftblib.lang.team.error.not_found", args[0]);
		}

		sender.addChatMessage(ServerLib.lang(sender, "commands.team.info.id", StringUtils.color(new ChatComponentText(team.getId()), EnumChatFormatting.BLUE)));
		sender.addChatMessage(ServerLib.lang(sender, "commands.team.info.uid", StringUtils.color(new ChatComponentText(team.getUID() + " / " + String.format("%04x", team.getUID())), EnumChatFormatting.BLUE)));
		sender.addChatMessage(ServerLib.lang(sender, "commands.team.info.owner", team.getOwner() == null ? "-" : StringUtils.color(team.getOwner().getDisplayName(), EnumChatFormatting.BLUE)));

		IChatComponent component = new ChatComponentText("");
		component.getChatStyle().setColor(EnumChatFormatting.GOLD);
		boolean first = true;

		for (ForgePlayer player : team.getMembers())
		{
			if (first)
			{
				first = false;
			}
			else
			{
				component.appendText(", ");
			}

			IChatComponent n = player.getDisplayName();
			n.getChatStyle().setColor(EnumChatFormatting.BLUE);
			component.appendSibling(n);
		}

		sender.addChatMessage(ServerLib.lang(sender, "commands.team.info.members", component));
	}
}
