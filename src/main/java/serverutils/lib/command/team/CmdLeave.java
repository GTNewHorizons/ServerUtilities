package serverutils.lib.command.team;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.lib.lib.command.CmdBase;
import serverutils.lib.lib.command.CommandUtils;
import serverutils.lib.lib.data.ForgePlayer;
import serverutils.mod.ServerUtilities;

public class CmdLeave extends CmdBase {

    public CmdLeave() {
        super("leave", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        ForgePlayer p = CommandUtils.getForgePlayer(getCommandSenderAsPlayer(sender));

        if (!p.hasTeam()) {
            throw ServerUtilities.error(sender, "serverutilities.lang.team.error.no_team");
        } else if (!p.team.removeMember(p)) {
            throw ServerUtilities.error(sender, "serverutilities.lang.team.error.must_transfer_ownership");
        }
    }
}
