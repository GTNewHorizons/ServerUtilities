package serverutils.serverlib.command.team;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.serverlib.ServerLib;
import serverutils.serverlib.lib.command.CmdEditConfigBase;
import serverutils.serverlib.lib.command.CommandUtils;
import serverutils.serverlib.lib.config.ConfigGroup;
import serverutils.serverlib.lib.config.IConfigCallback;
import serverutils.serverlib.lib.data.ServerLibAPI;
import serverutils.serverlib.lib.data.ForgePlayer;

public class CmdSettings extends CmdEditConfigBase {

	public CmdSettings() {
		super("settings", Level.ALL);
	}

	@Override
	public ConfigGroup getGroup(ICommandSender sender) throws CommandException {
		EntityPlayerMP player = getCommandSenderAsPlayer(sender);
		ForgePlayer p = CommandUtils.getForgePlayer(player);

		if (!p.hasTeam()) {
			ServerLibAPI.sendCloseGuiPacket(player);
			throw ServerLib.error(sender, "serverlib.lang.team.error.no_team");
		} else if (!p.team.isModerator(p)) {
			ServerLibAPI.sendCloseGuiPacket(player);
			throw new CommandException("commands.generic.permission");
		}

		return p.team.getSettings();
	}

	@Override
	public IConfigCallback getCallback(ICommandSender sender) throws CommandException {
		return CommandUtils.getForgePlayer(sender).team;
	}
}