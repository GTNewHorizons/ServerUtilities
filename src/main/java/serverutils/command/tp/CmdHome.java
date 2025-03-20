package serverutils.command.tp;

import static serverutils.ServerUtilitiesNotifications.TELEPORT;

import java.util.Collection;
import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesPermissions;
import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.data.TeleportType;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.Universe;
import serverutils.lib.math.BlockDimPos;
import serverutils.lib.util.permission.PermissionAPI;
import serverutils.task.NotifyTask;
import serverutils.task.Task;

public class CmdHome extends CmdBase {

    public CmdHome() {
        super("home", Level.ALL);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsFromIterableMatchingLastWord(
                    args,
                    ServerUtilitiesPlayerData.get(Universe.get().getPlayer(sender)).homes.list());
        }

        return super.addTabCompletionOptions(sender, args);
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 1;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args0) throws CommandException {
        if (args0.length == 0) {
            args0 = new String[] { "home" };
        }

        String[] args = args0;

        if (args[0].equals("list")) {
            ForgePlayer p = CommandUtils.getSelfOrOther(sender, args, 1, ServerUtilitiesPermissions.HOMES_LIST_OTHER);
            ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(p);

            Collection<String> list = data.homes.list();
            IChatComponent msg = p.getDisplayName().appendText(
                    ": " + list.size() + " / " + p.getRankConfig(ServerUtilitiesPermissions.HOMES_MAX).getInt() + ": ");

            if (!list.isEmpty()) {
                boolean first = true;

                for (String s : list) {
                    if (first) {
                        first = false;
                    } else {
                        msg.appendText(", ");
                    }

                    IChatComponent h = new ChatComponentText(s);
                    h.getChatStyle().setChatHoverEvent(
                            new HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    new ChatComponentText("/home " + s + " " + p.getName())));
                    h.getChatStyle().setChatClickEvent(
                            new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/home " + s + " " + p.getName()));
                    h.getChatStyle().setColor(EnumChatFormatting.GOLD);
                    msg.appendSibling(h);
                }
            }

            sender.addChatMessage(msg);

            return;
        } else if (args[0].equals("list_all")) {
            for (ForgePlayer p : Universe.get().getPlayers()) {
                processCommand(sender, new String[] { "list", p.getName() });
            }

            return;
        }

        ForgePlayer p = CommandUtils.getSelfOrOther(sender, args, 1, ServerUtilitiesPermissions.HOMES_TELEPORT_OTHER);
        BlockDimPos pos = ServerUtilitiesPlayerData.get(p).homes.get(args[0]);

        if (pos == null) {
            throw ServerUtilities.error(sender, "serverutilities.lang.homes.not_set", args[0]);
        }

        EntityPlayerMP player = getCommandSenderAsPlayer(sender);

        if (player.dimension != pos.dim
                && !PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.HOMES_CROSS_DIM)) {
            throw ServerUtilities.error(sender, "serverutilities.lang.homes.cross_dim");
        }

        Task task = new NotifyTask(-1, player, TELEPORT.createNotification("serverutilities.lang.warps.tp", args[0]));
        ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(sender);
        data.checkTeleportCooldown(sender, TeleportType.HOME);
        data.teleport(pos.teleporter(), TeleportType.HOME, task);
    }
}
