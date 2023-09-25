package serverutils.utils.command.tp;

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import com.feed_the_beast.ftblib.lib.command.CmdBase;
import com.feed_the_beast.ftblib.lib.command.CommandUtils;
import com.feed_the_beast.ftblib.lib.config.RankConfigAPI;
import com.feed_the_beast.ftblib.lib.data.Universe;
import com.feed_the_beast.ftblib.lib.math.BlockDimPos;
import serverutils.utils.ServerUtilities;
import serverutils.utils.ServerUtilitiesPermissions;
import serverutils.utils.data.FTBUtilitiesPlayerData;

public class CmdSetHome extends CmdBase {

    public CmdSetHome() {
        super("sethome", Level.ALL);
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
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);

        // if (player.isSpectator()) {
        // throw ServerUtilities.error(sender, "ftbutilities.lang.homes.spectator");
        // }

        FTBUtilitiesPlayerData data = FTBUtilitiesPlayerData.get(CommandUtils.getForgePlayer(player));

        if (args.length == 0) {
            args = new String[] { "home" };
        }

        args[0] = args[0].toLowerCase();

        int maxHomes = RankConfigAPI.get(player, ServerUtilitiesPermissions.HOMES_MAX).getInt();

        if (maxHomes <= 0 || data.homes.size() >= maxHomes) {
            if (maxHomes == 0 || data.homes.get(args[0]) == null) {
                throw ServerUtilities.error(sender, "ftbutilities.lang.homes.limit");
            }
        }

        data.homes.set(args[0], new BlockDimPos(sender));
        sender.addChatMessage(ServerUtilities.lang(sender, "ftbutilities.lang.homes.set", args[0]));
        data.player.markDirty();
    }
}
