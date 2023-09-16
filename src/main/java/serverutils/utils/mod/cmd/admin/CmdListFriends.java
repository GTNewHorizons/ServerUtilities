package serverutils.utils.mod.cmd.admin;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import latmod.lib.LMListUtils;
import serverutils.lib.api.cmd.CommandLM;
import serverutils.lib.api.cmd.CommandLevel;
import serverutils.utils.world.LMPlayerServer;

public class CmdListFriends extends CommandLM {

    public CmdListFriends() {
        super("list_friends", CommandLevel.OP);
    }

    public String getCommandUsage(ICommandSender ics) {
        return '/' + commandName + " <player>";
    }

    public Boolean getUsername(String[] args, int i) {
        return (i == 0) ? Boolean.TRUE : null;
    }

    public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
        checkArgs(args, 1);
        LMPlayerServer p = LMPlayerServer.get(args[0]);
        return new ChatComponentText(joinNiceString(LMListUtils.toStringArray(p.getFriends())));
    }
}
