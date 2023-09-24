package serverutils.serverlib.command.team;

import serverutils.serverlib.ServerLib;
import serverutils.serverlib.lib.EnumTeamStatus;
import serverutils.serverlib.lib.command.CmdBase;
import serverutils.serverlib.lib.command.CommandUtils;
import serverutils.serverlib.lib.data.ForgePlayer;
import serverutils.serverlib.lib.data.ForgeTeam;
import serverutils.serverlib.lib.data.Universe;
import serverutils.serverlib.lib.util.StringUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.event.ClickEvent;

import javax.annotation.Nullable;
import java.util.List;

public class CmdRequestInvite extends CmdBase
{
	public CmdRequestInvite()
	{
		super("request_invite", Level.ALL);
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos)
	{
		if (args.length == 1)
		{
			return getListOfStringsMatchingLastWord(args, EnumTeamStatus.VALID_VALUES);
		}

		return super.getTabCompletions(server, sender, args, pos);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
	{
		ForgePlayer p = CommandUtils.getForgePlayer(getCommandSenderAsPlayer(sender));

		if (p.hasTeam())
		{
			throw ServerLib.error(sender, "ftblib.lang.team.error.must_leave");
		}

		checkArgs(sender, args, 1);

		ForgeTeam team = Universe.get().getTeam(args[0]);

		if (!team.isValid())
		{
			throw ServerLib.error(sender, "error", args[0]);
		}

		team.setRequestingInvite(p, true);

		IChatComponent component = new ChatComponentText("");
		component.appendSibling(new ChatComponentTranslation("serverlib.lang.team.gui.members.requesting_invite"));
		component.appendText(": ");
		component.appendSibling(StringUtils.color(p.getDisplayName(), EnumChatFormatting.BLUE));
		component.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team status " + p.getName() + " member"));

		for (ForgePlayer player : team.getPlayersWithStatus(EnumTeamStatus.MOD))
		{
			if (player.isOnline())
			{
				player.getPlayer().addChatMessage(new ChatComponentTranslation("serverlib.lang.team.gui.members.requesting_invite"));
			}
		}
	}
}