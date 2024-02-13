package serverutils.command.tp;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import serverutils.ServerUtilities;
import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.data.Universe;

import java.util.List;

public class CmdHomes extends CmdBase {

    public CmdHomes() {
        super("homes", Level.ALL);
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

        sender.addChatMessage(ServerUtilities.lang(sender, "serverutilities.lang.homes.homes"));
        data.homes.list().forEach(home -> sender.addChatMessage(new ChatComponentText(home)));

        data.player.markDirty();
    }
}
