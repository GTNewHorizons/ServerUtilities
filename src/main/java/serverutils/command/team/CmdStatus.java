package serverutils.command.team;

import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.ServerUtilities;
import serverutils.lib.EnumTeamStatus;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.data.ForgePlayer;

public class CmdStatus extends CmdBase {

    public CmdStatus() {
        super("status", Level.ALL);
    }

    @Override
    public List<String> getCommandAliases() {
        return Collections.singletonList("set_status");
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 2) {
            return matchFromIterable(args, EnumTeamStatus.VALID_VALUES);
        }

        return super.addTabCompletionOptions(sender, args);
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        ForgePlayer p = CommandUtils.getForgePlayer(getCommandSenderAsPlayer(sender));

        if (!p.hasTeam()) {
            throw ServerUtilities.error(sender, "serverutilities.lang.team.error.no_team");
        } else if (!p.team.isModerator(p)) {
            throw new CommandException("commands.generic.permission");
        }

        checkArgs(sender, args, 1);
        ForgePlayer p1 = CommandUtils.getForgePlayer(sender, args[0]);

        if (args.length == 1) {
            sender.addChatMessage(EnumTeamStatus.NAME_MAP.getDisplayName(sender, p.team.getHighestStatus(p1)));
            return;
        }

        if (p.team.isOwner(p1)) {
            throw ServerUtilities.error(sender, "serverutilities.lang.team.permission.owner");
        } else if (!p.team.isModerator(p)) {
            throw new CommandException("commands.generic.permission");
        }

        EnumTeamStatus status = EnumTeamStatus.NAME_MAP.get(args[1].toLowerCase());

        if (status.canBeSet()) {
            p.team.setStatus(p1, status);
            sender.addChatMessage(
                    ServerUtilities.lang(
                            sender,
                            "commands.team.status.set",
                            p1.getDisplayName(),
                            EnumTeamStatus.NAME_MAP.getDisplayName(sender, status)));
        } else {
            sender.addChatMessage(ServerUtilities.lang(sender, "commands.team.status.cant_set"));
        }
    }
}
