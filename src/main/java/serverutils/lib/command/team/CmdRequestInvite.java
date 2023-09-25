package serverutils.lib.command.team;

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.lib.EnumTeamStatus;
import serverutils.lib.lib.command.CmdBase;
import serverutils.lib.lib.command.CommandUtils;
import serverutils.lib.lib.data.ForgePlayer;
import serverutils.lib.lib.data.ForgeTeam;
import serverutils.lib.lib.data.Universe;
import serverutils.lib.lib.util.StringUtils;

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
            throw ServerUtilitiesLib.error(sender, "serverlib.lang.team.error.must_leave");
        }

        checkArgs(sender, args, 1);

        ForgeTeam team = Universe.get().getTeam(args[0]);

        if (!team.isValid()) {
            throw ServerUtilitiesLib.error(sender, "error", args[0]);
        }

        team.setRequestingInvite(p, true);

        IChatComponent component = new ChatComponentText("");
        component.appendSibling(new ChatComponentTranslation("serverlib.lang.team.gui.members.requesting_invite"));
        component.appendText(": ");
        component.appendSibling(StringUtils.color(p.getDisplayName(), EnumChatFormatting.BLUE));
        component.getChatStyle().setChatClickEvent(
                new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team status " + p.getName() + " member"));

        for (ForgePlayer player : team.getPlayersWithStatus(EnumTeamStatus.MOD)) {
            if (player.isOnline()) {
                player.getPlayer().addChatMessage(
                        new ChatComponentTranslation("serverlib.lang.team.gui.members.requesting_invite"));
            }
        }
    }
}
