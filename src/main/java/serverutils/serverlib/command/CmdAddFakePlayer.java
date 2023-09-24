package serverutils.serverlib.command;

import serverutils.serverlib.ServerLib;
import serverutils.serverlib.lib.command.CmdBase;
import serverutils.serverlib.lib.data.ForgePlayer;
import serverutils.serverlib.lib.data.Universe;
import serverutils.serverlib.lib.util.StringUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/**
 * @author LatvianModder
 */
public class CmdAddFakePlayer extends CmdBase
{
	public CmdAddFakePlayer()
	{
		super("add_fake_player", Level.OP);
	}

	@Override
	public boolean isUsernameIndex(String[] args, int index)
	{
		return index == 0;
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
	{
		checkArgs(sender, args, 2);

		UUID id = StringUtils.fromString(args[0]);

		if (id == null)
		{
			throw ServerLib.error(sender, "serverlib.lang.add_fake_player.invalid_uuid");
		}

		if (Universe.get().getPlayer(id) != null || Universe.get().getPlayer(args[1]) != null)
		{
			throw ServerLib.error(sender, "serverlib.lang.add_fake_player.player_exists");
		}

		ForgePlayer p = new ForgePlayer(Universe.get(), id, args[1]);
		p.team.universe.players.put(p.getId(), p);
		p.clearCache();
		sender.addChatMessage(ServerLib.lang(sender, "serverlib.lang.add_fake_player.added", p.getDisplayName()));
	}
}
