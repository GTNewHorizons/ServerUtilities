package serverutils.old.mod.cmd.admin;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.IChatComponent;

import serverutils.lib.api.cmd.CommandLM;
import serverutils.lib.api.cmd.CommandLevel;
import serverutils.old.mod.ServerUtilities;
import serverutils.old.world.LMWorldServer;

public class CmdDelWarp extends CommandLM {

    public CmdDelWarp() {
        super("delwarp", CommandLevel.OP);
    }

    public String getCommandUsage(ICommandSender ics) {
        return '/' + commandName + " <ID>";
    }

    public String[] getTabStrings(ICommandSender ics, String args[], int i) throws CommandException {
        if (i == 0) return LMWorldServer.inst.warps.list();
        return super.getTabStrings(ics, args, i);
    }

    public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
        checkArgs(args, 1);
        if (LMWorldServer.inst.warps.set(args[0], null))
            return ServerUtilities.mod.chatComponent("cmd.warp_del", args[0]);
        return error(ServerUtilities.mod.chatComponent("cmd.warp_not_set", args[0]));
    }
}
