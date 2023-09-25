package serverutils.utils.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.lib.lib.command.CmdBase;
import serverutils.lib.lib.util.NBTUtils;
import serverutils.utils.data.ServerUtilitiesPlayerData;

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
        NBTUtils.getPersistedData(getPlayer(sender, args[0]), false).removeTag(ServerUtilitiesPlayerData.TAG_MUTED);
    }
}
