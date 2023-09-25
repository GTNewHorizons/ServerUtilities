package serverutils.utils.command;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;

import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.lib.command.CmdBase;
import serverutils.lib.lib.data.ForgePlayer;
import serverutils.lib.lib.data.Universe;
import serverutils.lib.lib.util.StringUtils;
import serverutils.utils.ServerUtilitiesCommon;
import serverutils.utils.data.Leaderboard;

public class CmdLeaderboard extends CmdBase {

    public CmdLeaderboard() {
        super("leaderboards", Level.ALL);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsFromIterableMatchingLastWord(args, ServerUtilitiesCommon.LEADERBOARDS.keySet());
        }

        return super.addTabCompletionOptions(sender, args);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            IChatComponent component = new ChatComponentText("");
            component.getChatStyle().setChatHoverEvent(
                    new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            StringUtils.color(ServerUtilitiesLib.lang(sender, "click_here"), EnumChatFormatting.GOLD)));
            boolean first = true;

            for (Leaderboard leaderboard : ServerUtilitiesCommon.LEADERBOARDS.values()) {
                if (first) {
                    first = false;
                } else {
                    component.appendText(", ");
                }

                IChatComponent component1 = leaderboard.getTitle().createCopy();
                component1.getChatStyle().setColor(EnumChatFormatting.GOLD);
                component1.getChatStyle().setChatClickEvent(
                        new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/leaderboards " + leaderboard.id));
                component.appendSibling(component1);
            }

            sender.addChatMessage(component);
        } else if (ServerUtilitiesCommon.LEADERBOARDS.get(new ResourceLocation(args[0])) != null) {
            Leaderboard leaderboard = ServerUtilitiesCommon.LEADERBOARDS.get(new ResourceLocation(args[0]));
            sender.addChatMessage(leaderboard.getTitle().createCopy().appendText(":"));

            ForgePlayer p0 = sender instanceof EntityPlayerMP ? Universe.get().getPlayer(sender) : null;
            List<ForgePlayer> players = new ArrayList<>(Universe.get().getPlayers());
            players.sort(leaderboard.getComparator());

            for (int i = 0; i < players.size(); i++) {
                ForgePlayer p = players.get(i);
                IChatComponent component = new ChatComponentText("#" + StringUtils.add0s(i + 1, players.size()) + " ")
                        .appendSibling(p.getDisplayName()).appendText(": ");
                component.appendSibling(leaderboard.createValue(p));

                if (p == p0) {
                    component.getChatStyle().setColor(EnumChatFormatting.DARK_GREEN);
                } else if (!leaderboard.hasValidValue(p)) {
                    component.getChatStyle().setColor(EnumChatFormatting.DARK_GRAY);
                } else if (i < 3) {
                    component.getChatStyle().setColor(EnumChatFormatting.GOLD);
                }

                sender.addChatMessage(component);
            }
        } else {
            sender.addChatMessage(new ChatComponentText("Invalid ID!")); // LANG
        }
    }
}
