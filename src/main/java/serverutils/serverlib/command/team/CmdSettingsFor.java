package serverutils.serverlib.command.team;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.serverlib.lib.command.CmdBase;
import serverutils.serverlib.lib.command.CommandUtils;
import serverutils.serverlib.lib.data.ServerLibAPI;
import serverutils.serverlib.lib.data.ForgeTeam;
import serverutils.serverlib.lib.data.Universe;

public class CmdSettingsFor extends CmdBase {

	public CmdSettingsFor() {
		super("settings_for", Level.OP);
	}

	@Override
	public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
		if (args.length == 1) {
			List<String> list = new ArrayList<>();

			for (ForgeTeam team : Universe.get().getTeams()) {
				if (team.type.isServer) {
					list.add(team.getId());
				}
			}

			return getListOfStringsFromIterableMatchingLastWord(args, list);
		}

		return super.addTabCompletionOptions(sender, args);
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) throws CommandException {
		checkArgs(sender, args, 1);
		ForgeTeam team = CommandUtils.getTeam(sender, args[0]);

		if (team.type.isServer) {
			ServerLibAPI.editServerConfig(getCommandSenderAsPlayer(sender), team.getSettings(), team);
		}
	}
}