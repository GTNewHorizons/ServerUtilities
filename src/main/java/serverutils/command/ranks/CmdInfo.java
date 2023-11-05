package serverutils.command.ranks;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import serverutils.ServerUtilities;
import serverutils.lib.command.CmdBase;
import serverutils.lib.util.StringUtils;
import serverutils.ranks.Rank;
import serverutils.ranks.Ranks;

public class CmdInfo extends CmdBase {

    public CmdInfo() {
        super("info", Level.ALL);
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return Ranks.isActive()
                    ? getListOfStringsFromIterableMatchingLastWord(args, Ranks.INSTANCE.getRankNames(false))
                    : Collections.emptyList();
        }

        return super.addTabCompletionOptions(sender, args);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (!Ranks.isActive()) {
            throw ServerUtilities.error(sender, "feature_disabled_server");
        }

        checkArgs(sender, args, 1);
        Rank rank = Ranks.INSTANCE.getRank(sender, args[0]);

        sender.addChatMessage(new ChatComponentText(""));
        IChatComponent id = new ChatComponentText(
                "[" + rank.getId() + (rank.comment.isEmpty() ? "]" : ("] - " + rank.comment)));
        id.getChatStyle().setColor(EnumChatFormatting.YELLOW);
        id.getChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, rank.getDisplayName()));
        sender.addChatMessage(id);

        Set<Rank> parents = rank.getParents();

        if (!parents.isEmpty()) {
            IChatComponent t = new ChatComponentText("");
            t.appendSibling(StringUtils.color(new ChatComponentText(Rank.NODE_PARENT), EnumChatFormatting.GOLD));
            t.appendText(": ");

            boolean first = true;

            for (Rank r : parents) {
                if (first) {
                    first = false;
                } else {
                    t.appendText(", ");
                }

                IChatComponent t1 = new ChatComponentText(r.getId());
                t1.getChatStyle().setColor(EnumChatFormatting.AQUA);

                if (!r.comment.isEmpty()) {
                    t1.getChatStyle().setChatHoverEvent(
                            new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(r.comment)));
                }

                t1.getChatStyle()
                        .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ranks info " + r.getId()));
                t.appendSibling(t1);
            }

            sender.addChatMessage(t);
        }

        for (Rank.Entry entry : rank.permissions.values()) {
            if (entry.node.equals(Rank.NODE_PARENT)) {
                continue;
            }

            IChatComponent t = new ChatComponentText("");
            t.appendSibling(StringUtils.color(new ChatComponentText(entry.node), EnumChatFormatting.GOLD));
            t.appendText(": ");
            t.appendSibling(StringUtils.color(new ChatComponentText(entry.value), EnumChatFormatting.BLUE));

            if (!entry.comment.isEmpty()) {
                t.getChatStyle().setChatHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(entry.comment)));
            }

            t.getChatStyle().setChatClickEvent(
                    new ClickEvent(
                            ClickEvent.Action.SUGGEST_COMMAND,
                            "/ranks set_permission " + rank.getId() + " " + entry.node + " " + entry.value));
            sender.addChatMessage(t);
        }
    }
}
