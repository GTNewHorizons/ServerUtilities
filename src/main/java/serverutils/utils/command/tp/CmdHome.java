package serverutils.utils.command.tp;

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

import com.feed_the_beast.ftblib.lib.command.CmdBase;
import com.feed_the_beast.ftblib.lib.command.CommandUtils;
import com.feed_the_beast.ftblib.lib.data.ForgePlayer;
import com.feed_the_beast.ftblib.lib.data.Universe;
import com.feed_the_beast.ftblib.lib.math.BlockDimPos;
import com.feed_the_beast.ftblib.lib.util.permission.PermissionAPI;
import com.feed_the_beast.ftblib.lib.util.text_components.Notification;
import serverutils.utils.ServerUtilities;
import serverutils.utils.ServerUtilitiesNotifications;
import serverutils.utils.ServerUtilitiesPermissions;
import serverutils.utils.data.FTBUtilitiesPlayerData;

public class CmdHome extends CmdBase {

    public CmdHome() {
        super("home", Level.ALL);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsFromIterableMatchingLastWord(
                    args,
                    FTBUtilitiesPlayerData.get(Universe.get().getPlayer(sender)).homes.list());
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
            FTBUtilitiesPlayerData data = FTBUtilitiesPlayerData.get(p);

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
        FTBUtilitiesPlayerData data = FTBUtilitiesPlayerData.get(p);
        BlockDimPos pos = data.homes.get(args[0]);

        if (pos == null) {
            throw ServerUtilities.error(sender, "ftbutilities.lang.homes.not_set", args[0]);
        }

        EntityPlayerMP player = getCommandSenderAsPlayer(sender);

        if (player.dimension != pos.dim
                && !PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.HOMES_CROSS_DIM)) {
            throw ServerUtilities.error(sender, "ftbutilities.lang.homes.cross_dim");
        }

        data.checkTeleportCooldown(sender, FTBUtilitiesPlayerData.Timer.HOME);
        FTBUtilitiesPlayerData.Timer.HOME.teleport(
                player,
                playerMP -> pos.teleporter(),
                universe -> Notification
                        .of(
                                ServerUtilitiesNotifications.TELEPORT,
                                ServerUtilities.lang(sender, "ftbutilities.lang.warps.tp", args[0]))
                        .send(player.mcServer, player));
    }
}
