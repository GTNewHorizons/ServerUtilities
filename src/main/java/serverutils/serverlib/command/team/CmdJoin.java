package serverutils.serverlib.command.team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.serverlib.ServerLib;
import serverutils.serverlib.ServerLibGameRules;
import serverutils.serverlib.events.team.ForgeTeamChangedEvent;
import serverutils.serverlib.lib.command.CmdBase;
import serverutils.serverlib.lib.command.CommandUtils;
import serverutils.serverlib.lib.data.ForgePlayer;
import serverutils.serverlib.lib.data.ForgeTeam;
import serverutils.serverlib.lib.data.Universe;

public class CmdJoin extends CmdBase {

	public CmdJoin() {
		super("join", Level.ALL);
	}

	@Override
	public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
		if (args.length == 1) {
			if (!ServerLibGameRules.canJoinTeam(sender.getEntityWorld())) {
				return Collections.emptyList();
			}

			List<String> list = new ArrayList<>();

			try {
				ForgePlayer player = CommandUtils.getForgePlayer(sender);

				for (ForgeTeam team : Universe.get().getTeams()) {
					if (team.addMember(player, true)) {
						list.add(team.getId());
					}
				}

				if (list.size() > 1) {
					list.sort(null);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			return getListOfStringsFromIterableMatchingLastWord(args, list);
		}

		return super.addTabCompletionOptions(sender, args);
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) throws CommandException {
		if (!ServerLibGameRules.canJoinTeam(sender.getEntityWorld())) {
			throw ServerLib.error(sender, "feature_disabled_server");
		}

		EntityPlayerMP player = getCommandSenderAsPlayer(sender);
		ForgePlayer p = CommandUtils.getForgePlayer(player);

		checkArgs(sender, args, 1);

		ForgeTeam team = CommandUtils.getTeam(sender, args[0]);

		if (team.addMember(p, true)) {
			if (p.team.isOwner(p)) {
				new ForgeTeamChangedEvent(team, p.team).post();
				p.team.removeMember(p);
			} else if (p.hasTeam()) {
				throw ServerLib.error(sender, "serverlib.lang.team.error.must_leave");
			}

			team.addMember(p, false);
		} else {
			throw ServerLib.error(sender, "serverlib.lang.team.error.already_member", p.getDisplayName());
		}
	}
}
