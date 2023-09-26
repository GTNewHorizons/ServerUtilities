package serverutils.utils.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.lib.lib.command.CmdBase;
import serverutils.lib.lib.util.NBTUtils;
import serverutils.utils.ServerUtilities;
import serverutils.utils.data.ServerUtilitiesPlayerData;

public class CmdMute extends CmdBase {

    public CmdMute() {
        super("mute", Level.OP);
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        checkArgs(sender, args, 1);
        NBTUtils.getPersistedData(getPlayer(sender, args[0]), true)
                .setBoolean(ServerUtilitiesPlayerData.TAG_MUTED, true);
        sender.addChatMessage(ServerUtilities.lang(sender, "serverutilities.lang.muted", getPlayer(sender, args[0]).getDisplayName()));
    }
}
