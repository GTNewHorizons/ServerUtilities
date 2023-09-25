package serverutils.utils.command.tp;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import com.feed_the_beast.ftblib.FTBLib;
import com.feed_the_beast.ftblib.lib.command.CmdBase;
import com.feed_the_beast.ftblib.lib.command.CommandUtils;
import com.feed_the_beast.ftblib.lib.data.Universe;
import com.feed_the_beast.ftblib.lib.util.StringUtils;
import com.feed_the_beast.ftblib.lib.util.misc.TimeType;
import serverutils.utils.ServerUtilities;
import serverutils.utils.data.FTBUtilitiesPlayerData;

/**
 * @author LatvianModder
 */
public class CmdTPA extends CmdBase {

    public CmdTPA() {
        super("tpa", Level.ALL);
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        checkArgs(sender, args, 1);
        FTBUtilitiesPlayerData self = FTBUtilitiesPlayerData.get(CommandUtils.getForgePlayer(sender));

        self.checkTeleportCooldown(sender, FTBUtilitiesPlayerData.Timer.TPA);

        FTBUtilitiesPlayerData other = FTBUtilitiesPlayerData.get(CommandUtils.getForgePlayer(sender, args[0]));

        IChatComponent selfName = StringUtils
                .color(new ChatComponentText(self.player.getPlayer().getDisplayName()), EnumChatFormatting.BLUE);
        IChatComponent otherName = StringUtils
                .color(new ChatComponentText(other.player.getPlayer().getDisplayName()), EnumChatFormatting.BLUE);

        if (self.player.equalsPlayer(other.player) || !other.player.isOnline()
                || other.tpaRequestsFrom.contains(self.player)) {
            IChatComponent component = ServerUtilities.lang(sender, "ftbutilities.lang.tpa.cant_request");
            component.getChatStyle().setColor(EnumChatFormatting.RED);
            component.getChatStyle().setChatHoverEvent(
                    new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            ServerUtilities.lang(sender, "ftbutilities.lang.tpa.from_to", selfName, otherName)));
            sender.addChatMessage(component);
            return;
        }

        IChatComponent c = ServerUtilities.lang(sender, "ftbutilities.lang.tpa.request_sent");
        c.getChatStyle().setChatHoverEvent(
                new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        ServerUtilities.lang(sender, "ftbutilities.lang.tpa.from_to", selfName, otherName)));
        sender.addChatMessage(c);

        other.tpaRequestsFrom.add(self.player);

        IChatComponent accept = FTBLib.lang(other.player.getPlayer(), "click_here");
        accept.getChatStyle().setColor(EnumChatFormatting.GOLD);
        accept.getChatStyle()
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept " + self.player.getName()));
        accept.getChatStyle().setChatHoverEvent(
                new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new ChatComponentText("/tpaccept " + self.player.getName())));

        other.player.getPlayer().addChatMessage(
                ServerUtilities
                        .lang(other.player.getPlayer(), "ftbutilities.lang.tpa.request_received", selfName, accept));

        Universe.get().scheduleTask(TimeType.MILLIS, System.currentTimeMillis() + 30000L, universe -> {
            if (other.tpaRequestsFrom.remove(self.player)) {
                IChatComponent component = ServerUtilities.lang(sender, "ftbutilities.lang.tpa.request_expired");
                component.getChatStyle().setChatHoverEvent(
                        new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                ServerUtilities.lang(sender, "ftbutilities.lang.tpa.from_to", selfName, otherName)));
                sender.addChatMessage(component);

                if (other.player.isOnline()) {
                    component = ServerUtilities.lang(other.player.getPlayer(), "ftbutilities.lang.tpa.request_expired");
                    component.getChatStyle().setChatHoverEvent(
                            new HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    ServerUtilities.lang(
                                            other.player.getPlayer(),
                                            "ftbutilities.lang.tpa.from_to",
                                            selfName,
                                            otherName)));
                    other.player.getPlayer().addChatMessage(component);
                }
            }
        });
    }
}
