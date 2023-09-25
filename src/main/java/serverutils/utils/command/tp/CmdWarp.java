package serverutils.utils.command.tp;

import java.util.Collection;
import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;

import serverutils.lib.lib.command.CmdBase;
import serverutils.lib.lib.command.CommandUtils;
import serverutils.lib.lib.math.BlockDimPos;
import serverutils.lib.lib.util.StringJoiner;
import serverutils.lib.lib.util.text_components.Notification;
import serverutils.utils.ServerUtilities;
import serverutils.utils.ServerUtilitiesNotifications;
import serverutils.utils.data.ServerUtilitiesPlayerData;
import serverutils.utils.data.ServerUtilitiesUniverseData;
import serverutils.utils.ranks.Rank;
import serverutils.utils.ranks.Ranks;

import cpw.mods.fml.common.eventhandler.Event;

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
        data.checkTeleportCooldown(sender, ServerUtilitiesPlayerData.Timer.WARP);
        ServerUtilitiesPlayerData.Timer.WARP.teleport(
                player,
                playerMP -> p.teleporter(),
                universe -> Notification
                        .of(
                                ServerUtilitiesNotifications.TELEPORT,
                                ServerUtilities.lang(sender, "serverutilities.lang.warps.tp", args[0]))
                        .send(player.mcServer, player));
    }
}
