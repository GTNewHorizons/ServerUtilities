package serverutils.lib.command;

import java.util.UUID;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.lib.lib.command.CmdBase;
import serverutils.lib.lib.data.ForgePlayer;
import serverutils.lib.lib.data.Universe;
import serverutils.lib.lib.util.StringUtils;
import serverutils.lib.ServerUtilitiesLib;

public class CmdAddFakePlayer extends CmdBase {

	public CmdAddFakePlayer() {
		super("add_fake_player", Level.OP);
	}

	@Override
	public boolean isUsernameIndex(String[] args, int index) {
		return index == 0;
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) throws CommandException {
		checkArgs(sender, args, 2);

		UUID id = StringUtils.fromString(args[0]);

		if (id == null) {
			throw ServerUtilitiesLib.error(sender, "serverlib.lang.add_fake_player.invalid_uuid");
		}

		if (Universe.get().getPlayer(id) != null || Universe.get().getPlayer(args[1]) != null) {
			throw ServerUtilitiesLib.error(sender, "serverlib.lang.add_fake_player.player_exists");
		}

		ForgePlayer p = new ForgePlayer(Universe.get(), id, args[1]);
		p.team.universe.players.put(p.getId(), p);
		p.clearCache();
		sender.addChatMessage(ServerUtilitiesLib.lang(sender, "serverlib.lang.add_fake_player.added", p.getDisplayName()));
	}
}
