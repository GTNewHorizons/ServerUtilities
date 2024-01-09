package serverutils.command.tp;

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesPermissions;
import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.config.RankConfigAPI;
import serverutils.lib.data.Universe;
import serverutils.lib.math.BlockDimPos;

public class CmdSetHome extends CmdBase {

    public CmdSetHome() {
        super("sethome", Level.ALL);
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
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(CommandUtils.getForgePlayer(player));

        if (args.length == 0) {
            args = new String[] { "home" };
        }

        args[0] = args[0].toLowerCase();

        int maxHomes = RankConfigAPI.get(player, ServerUtilitiesPermissions.HOMES_MAX).getInt();

        if (maxHomes <= 0 || data.homes.size() >= maxHomes) {
            if (maxHomes == 0 || data.homes.get(args[0]) == null) {
                throw ServerUtilities.error(sender, "serverutilities.lang.homes.limit");
            }
        }

        data.homes.set(args[0], new BlockDimPos(sender));
        sender.addChatMessage(ServerUtilities.lang(sender, "serverutilities.lang.homes.set", args[0]));
        data.player.markDirty();
    }
}
