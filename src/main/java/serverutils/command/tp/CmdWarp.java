package serverutils.command.tp;

import static serverutils.ServerUtilitiesNotifications.TELEPORT;

import java.util.Collection;
import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;

import cpw.mods.fml.common.eventhandler.Event;
import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesPermissions;
import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.data.ServerUtilitiesUniverseData;
import serverutils.data.TeleportType;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.math.BlockDimPos;
import serverutils.lib.util.StringJoiner;
import serverutils.lib.util.permission.PermissionAPI;
import serverutils.ranks.Rank;
import serverutils.ranks.Ranks;
import serverutils.task.NotifyTask;
import serverutils.task.Task;

public class CmdWarp extends CmdBase {

    public CmdWarp() {
        super("warp", Level.ALL);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsFromIterableMatchingLastWord(args, ServerUtilitiesUniverseData.WARPS.list());
        }

        return super.addTabCompletionOptions(sender, args);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        checkArgs(sender, args, 1);

        args[0] = args[0].toLowerCase();

        if (args[0].equals("list")) {
            Collection<String> list = ServerUtilitiesUniverseData.WARPS.list();
            sender.addChatMessage(new ChatComponentText(list.isEmpty() ? "-" : StringJoiner.with(", ").join(list)));
            return;
        }

        EntityPlayerMP player = getCommandSenderAsPlayer(sender);

        if (Ranks.INSTANCE
                .getPermissionResult(player, Rank.NODE_COMMAND + ".serverutilities.warp.teleport." + args[0], true)
                == Event.Result.DENY) {
            throw new CommandException("commands.generic.permission");
        }

        BlockDimPos p = ServerUtilitiesUniverseData.WARPS.get(args[0]);

        if (p == null) {
            throw ServerUtilities.error(sender, "serverutilities.lang.warps.not_set", args[0]);
        }

        ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(CommandUtils.getForgePlayer(player));
        data.checkTeleportCooldown(sender, TeleportType.WARP);

        if (player.dimension != p.dim
                && !PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.WARPS_CROSS_DIM)) {
            throw ServerUtilities.error(sender, "serverutilities.lang.warps.cross_dim");
        }

        Task task = new NotifyTask(-1, player, TELEPORT.createNotification("serverutilities.lang.warps.tp", args[0]));
        data.teleport(p.teleporter(), TeleportType.WARP, task);
    }
}
