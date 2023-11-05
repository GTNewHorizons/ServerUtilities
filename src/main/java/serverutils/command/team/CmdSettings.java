package serverutils.command.team;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.ServerUtilities;
import serverutils.lib.command.CmdEditConfigBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.config.ConfigGroup;
import serverutils.lib.config.IConfigCallback;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.ServerUtilitiesAPI;

public class CmdSettings extends CmdEditConfigBase {

    public CmdSettings() {
        super("settings", Level.ALL);
    }

    @Override
    public ConfigGroup getGroup(ICommandSender sender) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ForgePlayer p = CommandUtils.getForgePlayer(player);

        if (!p.hasTeam()) {
            ServerUtilitiesAPI.sendCloseGuiPacket(player);
            throw ServerUtilities.error(sender, "serverutilities.lang.team.error.no_team");
        } else if (!p.team.isModerator(p)) {
            ServerUtilitiesAPI.sendCloseGuiPacket(player);
            throw new CommandException("commands.generic.permission");
        }

        return p.team.getSettings();
    }

    @Override
    public IConfigCallback getCallback(ICommandSender sender) throws CommandException {
        return CommandUtils.getForgePlayer(sender).team;
    }
}
