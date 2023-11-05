package serverutils.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.ServerUtilities;
import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.lib.command.CmdBase;
import serverutils.lib.util.NBTUtils;

public class CmdUnmute extends CmdBase {

    public CmdUnmute() {
        super("unmute", Level.OP);
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        checkArgs(sender, args, 1);
        NBTUtils.getPersistedData(getPlayer(sender, args[0]), false).removeTag(ServerUtilitiesPlayerData.TAG_MUTED);
        sender.addChatMessage(
                ServerUtilities
                        .lang(sender, "serverutilities.lang.unmuted", getPlayer(sender, args[0]).getDisplayName()));
    }
}
