package serverutils.lib.command.team;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.lib.lib.command.CmdEditConfigBase;
import serverutils.lib.lib.command.CommandUtils;
import serverutils.lib.lib.config.ConfigGroup;
import serverutils.lib.lib.config.IConfigCallback;
import serverutils.lib.lib.data.ForgePlayer;
import serverutils.lib.lib.data.ServerUtilitiesLibAPI;
import serverutils.mod.ServerUtilities;

public class CmdSettings extends CmdEditConfigBase {

    public CmdSettings() {
        super("settings", Level.ALL);
    }

    @Override
    public ConfigGroup getGroup(ICommandSender sender) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ForgePlayer p = CommandUtils.getForgePlayer(player);

        if (!p.hasTeam()) {
            ServerUtilitiesLibAPI.sendCloseGuiPacket(player);
            throw ServerUtilities.error(sender, "serverutilities.lang.team.error.no_team");
        } else if (!p.team.isModerator(p)) {
            ServerUtilitiesLibAPI.sendCloseGuiPacket(player);
            throw new CommandException("commands.generic.permission");
        }

        return p.team.getSettings();
    }

    @Override
    public IConfigCallback getCallback(ICommandSender sender) throws CommandException {
        return CommandUtils.getForgePlayer(sender).team;
    }
}
