package serverutils.command.team;

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import serverutils.ServerUtilities;
import serverutils.lib.EnumTeamStatus;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.ForgeTeam;
import serverutils.lib.data.Universe;
import serverutils.lib.util.StringUtils;

public class CmdRequestInvite extends CmdBase {

    public CmdRequestInvite() {
        super("request_invite", Level.ALL);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return matchFromIterable(args, EnumTeamStatus.VALID_VALUES);
        }

        return super.addTabCompletionOptions(sender, args);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        ForgePlayer p = CommandUtils.getForgePlayer(getCommandSenderAsPlayer(sender));

        if (p.hasTeam()) {
            throw ServerUtilities.error(sender, "serverutilities.lang.team.error.must_leave");
        }

        checkArgs(sender, args, 1);

        ForgeTeam team = Universe.get().getTeam(args[0]);

        if (!team.isValid()) {
            throw ServerUtilities.error(sender, "error", args[0]);
        }

        team.setRequestingInvite(p, true);

        IChatComponent component = new ChatComponentText("");
        component
                .appendSibling(new ChatComponentTranslation("serverutilities.lang.team.gui.members.requesting_invite"));
        component.appendText(": ");
        component.appendSibling(StringUtils.color(p.getDisplayName(), EnumChatFormatting.BLUE));
        component.getChatStyle().setChatClickEvent(
                new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team status " + p.getName() + " member"));

        for (ForgePlayer player : team.getPlayersWithStatus(EnumTeamStatus.MOD)) {
            if (player.isOnline()) {
                player.getPlayer().addChatMessage(
                        new ChatComponentTranslation("serverutilities.lang.team.gui.members.requesting_invite"));
            }
        }
    }
}
