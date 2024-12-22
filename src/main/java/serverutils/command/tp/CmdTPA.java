package serverutils.command.tp;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import serverutils.ServerUtilities;
import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.data.TeleportType;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.data.Universe;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.StringUtils;
import serverutils.task.Task;

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
        ServerUtilitiesPlayerData self = ServerUtilitiesPlayerData.get(CommandUtils.getForgePlayer(sender));

        self.checkTeleportCooldown(sender, TeleportType.TPA);

        ServerUtilitiesPlayerData other = ServerUtilitiesPlayerData.get(CommandUtils.getForgePlayer(sender, args[0]));

        IChatComponent selfName = StringUtils
                .color(new ChatComponentText(self.player.getPlayer().getDisplayName()), EnumChatFormatting.BLUE);

        if (self.player.equalsPlayer(other.player) || !other.player.isOnline()
                || other.tpaRequestsFrom.contains(self.player)) {
            IChatComponent component = ServerUtilities.lang(sender, "serverutilities.lang.tpa.cant_request");
            component.getChatStyle().setColor(EnumChatFormatting.RED);
            component.getChatStyle().setChatHoverEvent(
                    new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            ServerUtilities.lang(sender, "serverutilities.lang.tpa.from_to", selfName, args[0])));
            sender.addChatMessage(component);
            return;
        }

        IChatComponent otherName = StringUtils
                .color(new ChatComponentText(other.player.getPlayer().getDisplayName()), EnumChatFormatting.BLUE);
        IChatComponent c = ServerUtilities.lang(sender, "serverutilities.lang.tpa.request_sent");
        c.getChatStyle().setChatHoverEvent(
                new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        ServerUtilities.lang(sender, "serverutilities.lang.tpa.from_to", selfName, otherName)));
        sender.addChatMessage(c);

        other.tpaRequestsFrom.add(self.player);

        IChatComponent accept = ServerUtilities.lang(other.player.getPlayer(), "click_here");
        accept.getChatStyle().setColor(EnumChatFormatting.GOLD);
        accept.getChatStyle()
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept " + self.player.getName()));
        accept.getChatStyle().setChatHoverEvent(
                new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new ChatComponentText("/tpaccept " + self.player.getName())));

        other.player.getPlayer().addChatMessage(
                ServerUtilities
                        .lang(other.player.getPlayer(), "serverutilities.lang.tpa.request_received", selfName, accept));

        Task task = new Task(System.currentTimeMillis() + Ticks.SECOND.x(30).millis()) {

            @Override
            public void execute(Universe universe) {
                if (other.tpaRequestsFrom.remove(self.player)) {
                    IChatComponent component = ServerUtilities.lang(sender, "serverutilities.lang.tpa.request_expired");
                    component.getChatStyle().setChatHoverEvent(
                            new HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    ServerUtilities
                                            .lang(sender, "serverutilities.lang.tpa.from_to", selfName, otherName)));
                    sender.addChatMessage(component);

                    if (other.player.isOnline()) {
                        component = ServerUtilities
                                .lang(other.player.getPlayer(), "serverutilities.lang.tpa.request_expired");
                        component.getChatStyle().setChatHoverEvent(
                                new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        ServerUtilities.lang(
                                                other.player.getPlayer(),
                                                "serverutilities.lang.tpa.from_to",
                                                selfName,
                                                otherName)));
                        other.player.getPlayer().addChatMessage(component);
                    }
                }
            }
        };
        Universe.get().scheduleTask(task);
    }
}
