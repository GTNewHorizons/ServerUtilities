package serverutils.lib.command.team;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.lib.command.CmdBase;
import serverutils.lib.lib.command.CommandUtils;
import serverutils.lib.lib.data.ForgePlayer;

public class CmdKick extends CmdBase {

    public CmdKick() {
        super("kick", Level.ALL);
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        ForgePlayer p = CommandUtils.getForgePlayer(getCommandSenderAsPlayer(sender));

        if (!p.hasTeam()) {
            throw ServerUtilitiesLib.error(sender, "serverlib.lang.team.error.no_team");
        } else if (!p.team.isModerator(p)) {
            throw new CommandException("commands.generic.permission");
        }

        checkArgs(sender, args, 1);

        ForgePlayer p1 = CommandUtils.getForgePlayer(sender, args[0]);

        if (!p.team.isMember(p1)) {
            throw ServerUtilitiesLib.error(sender, "serverlib.lang.team.error.not_member", p1.getDisplayName());
        } else if (!p1.equalsPlayer(p)) {
            p.team.removeMember(p1);
        } else {
            throw ServerUtilitiesLib.error(sender, "serverlib.lang.team.error.must_transfer_ownership");
        }
    }
}
