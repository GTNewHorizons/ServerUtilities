package serverutils.command.team;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesGameRules;
import serverutils.events.team.ForgeTeamCreatedEvent;
import serverutils.events.team.ForgeTeamPlayerJoinedEvent;
import serverutils.lib.EnumTeamColor;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.ForgeTeam;
import serverutils.lib.data.TeamType;
import serverutils.net.MessageMyTeamGuiResponse;

public class CmdCreate extends CmdBase {

    public CmdCreate() {
        super("create", Level.ALL);
    }

    public static boolean isValidTeamID(String s) {
        if (!s.isEmpty()) {
            for (int i = 0; i < s.length(); i++) {
                if (!isValidChar(s.charAt(i))) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    private static boolean isValidChar(char c) {
        return c == '_' || c == '|' || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (!ServerUtilitiesGameRules.canCreateTeam(sender.getEntityWorld())) {
            throw ServerUtilities.error(sender, "feature_disabled_server");
        }

        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ForgePlayer p = CommandUtils.getForgePlayer(player);

        if (p.hasTeam()) {
            throw ServerUtilities.error(sender, "serverutilities.lang.team.error.must_leave");
        }

        checkArgs(sender, args, 1);

        if (!isValidTeamID(args[0])) {
            throw ServerUtilities.error(sender, "serverutilities.lang.team.id_invalid");
        }

        if (p.team.universe.getTeam(args[0]).isValid()) {
            throw ServerUtilities.error(sender, "serverutilities.lang.team.id_already_exists");
        }

        p.team.universe.clearCache();

        ForgeTeam team = new ForgeTeam(
                p.team.universe,
                p.team.universe.generateTeamUID((short) 0),
                args[0],
                TeamType.PLAYER);

        if (args.length > 1) {
            team.setColor(EnumTeamColor.NAME_MAP.get(args[1]));
        } else {
            team.setColor(EnumTeamColor.NAME_MAP.getRandom(sender.getEntityWorld().rand));
        }

        p.team = team;
        team.owner = p;
        team.universe.addTeam(team);
        new ForgeTeamCreatedEvent(team).post();
        ForgeTeamPlayerJoinedEvent event = new ForgeTeamPlayerJoinedEvent(p);
        event.post();
        sender.addChatMessage(ServerUtilities.lang(sender, "serverutilities.lang.team.created", team.getId()));

        if (event.getDisplayGui() != null) {
            event.getDisplayGui().run();
        } else {
            new MessageMyTeamGuiResponse(p).sendTo(player);
        }

        team.markDirty();
        p.markDirty();
    }
}
