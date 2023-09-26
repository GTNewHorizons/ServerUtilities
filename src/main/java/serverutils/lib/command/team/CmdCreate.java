package serverutils.lib.command.team;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.ServerUtilitiesLibGameRules;
import serverutils.lib.events.team.ForgeTeamCreatedEvent;
import serverutils.lib.events.team.ForgeTeamPlayerJoinedEvent;
import serverutils.lib.lib.EnumTeamColor;
import serverutils.lib.lib.command.CmdBase;
import serverutils.lib.lib.command.CommandUtils;
import serverutils.lib.lib.data.ForgePlayer;
import serverutils.lib.lib.data.ForgeTeam;
import serverutils.lib.lib.data.TeamType;
import serverutils.lib.net.MessageMyTeamGuiResponse;

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
        if (!ServerUtilitiesLibGameRules.canCreateTeam(sender.getEntityWorld())) {
            throw ServerUtilitiesLib.error(sender, "feature_disabled_server");
        }

        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ForgePlayer p = CommandUtils.getForgePlayer(player);

        if (p.hasTeam()) {
            throw ServerUtilitiesLib.error(sender, "serverutilitieslib.lang.team.error.must_leave");
        }

        checkArgs(sender, args, 1);

        if (!isValidTeamID(args[0])) {
            throw ServerUtilitiesLib.error(sender, "serverutilitieslib.lang.team.id_invalid");
        }

        if (p.team.universe.getTeam(args[0]).isValid()) {
            throw ServerUtilitiesLib.error(sender, "serverutilitieslib.lang.team.id_already_exists");
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
        sender.addChatMessage(ServerUtilitiesLib.lang(sender, "serverutilitieslib.lang.team.created", team.getId()));

        if (event.getDisplayGui() != null) {
            event.getDisplayGui().run();
        } else {
            new MessageMyTeamGuiResponse(p).sendTo(player);
        }

        team.markDirty();
        p.markDirty();
    }
}
