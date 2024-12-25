package serverutils.command.team;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentTranslation;

import serverutils.ServerUtilities;
import serverutils.lib.EnumTeamStatus;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.ForgeTeam;
import serverutils.lib.data.Universe;

public class CmdStatusFor extends CmdBase {

    public CmdStatusFor() {
        super("status_for", Level.OP);
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 1;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> {
                List<String> list = new ArrayList<>();
                for (ForgeTeam team : Universe.get().getTeams()) {
                    list.add(team.getId());
                }
                yield getListOfStringsFromIterableMatchingLastWord(args, list);
            }
            case 3 -> matchFromIterable(args, EnumTeamStatus.VALID_VALUES);
            default -> super.addTabCompletionOptions(sender, args);
        };
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        checkArgs(sender, args, 3);
        ForgeTeam team = CommandUtils.getTeam(sender, args[0]);
        ForgePlayer player = CommandUtils.getForgePlayer(sender, args[1]);
        EnumTeamStatus status = EnumTeamStatus.NAME_MAP.getNullable(args[2]);

        if (status == null) {
            throw ServerUtilities.error(sender, "commands.team.status.invalid", args[2], EnumTeamStatus.VALID_VALUES);
        }

        if (team.setStatus(player, status)) {
            sender.addChatMessage(
                    new ChatComponentTranslation(
                            "commands.team.status.success",
                            player.getName(),
                            team.getId(),
                            status.getName()));
        } else {
            throw ServerUtilities
                    .error(sender, "commands.team.status.failed", status.getName(), player.getName(), team.getId());
        }
    }
}
