package serverutils.command.team;

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import serverutils.ServerUtilities;
import serverutils.lib.command.CmdBase;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.ForgeTeam;
import serverutils.lib.data.Universe;
import serverutils.lib.util.StringUtils;

public class CmdInfo extends CmdBase {

    public CmdInfo() {
        super("info", Level.ALL);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return matchFromIterable(args, Universe.get().getTeams());
        }

        return super.addTabCompletionOptions(sender, args);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        checkArgs(sender, args, 1);
        ForgeTeam team = Universe.get().getTeam(args[0]);

        if (!team.isValid()) {
            throw ServerUtilities.error(sender, "serverutilities.lang.team.error.not_found", args[0]);
        }

        sender.addChatMessage(
                ServerUtilities.lang(
                        sender,
                        "commands.team.info.id",
                        StringUtils.color(new ChatComponentText(team.getId()), EnumChatFormatting.BLUE)));
        sender.addChatMessage(
                ServerUtilities.lang(
                        sender,
                        "commands.team.info.uid",
                        StringUtils.color(
                                new ChatComponentText(team.getUID() + " / " + String.format("%04x", team.getUID())),
                                EnumChatFormatting.BLUE)));
        sender.addChatMessage(
                ServerUtilities.lang(
                        sender,
                        "commands.team.info.owner",
                        team.getOwner() == null ? "-"
                                : StringUtils.color(team.getOwner().getDisplayName(), EnumChatFormatting.BLUE)));

        IChatComponent component = new ChatComponentText("");
        component.getChatStyle().setColor(EnumChatFormatting.GOLD);
        boolean first = true;

        for (ForgePlayer player : team.getMembers()) {
            if (first) {
                first = false;
            } else {
                component.appendText(", ");
            }

            IChatComponent n = player.getDisplayName();
            n.getChatStyle().setColor(EnumChatFormatting.BLUE);
            component.appendSibling(n);
        }

        sender.addChatMessage(ServerUtilities.lang(sender, "commands.team.info.members", component));
    }
}
