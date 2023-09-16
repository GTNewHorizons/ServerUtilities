package serverutils.utils.mod.cmd.admin;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import latmod.lib.LMStringUtils;
import serverutils.lib.BlockDimPos;
import serverutils.lib.LMDimUtils;
import serverutils.lib.api.cmd.CommandLevel;
import serverutils.lib.api.cmd.CommandSubLM;
import serverutils.utils.mod.ServerUtilities;
import serverutils.utils.world.LMPlayerServer;

public class CmdAdminHome extends CommandSubLM {

    public CmdAdminHome() {
        super("home", CommandLevel.OP);
    }

    public String getCommandUsage(ICommandSender ics) {
        return '/' + commandName + " <ID> [x] [y] [z]";
    }

    public Boolean getUsername(String[] args, int i) {
        if (i == 0) return Boolean.FALSE;
        return null;
    }

    public String[] getTabStrings(ICommandSender ics, String args[], int i) throws CommandException {
        if (i == 1) return new String[] { "list", "tp", "remove" };
        return super.getTabStrings(ics, args, i);
    }

    public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
        checkArgs(args, 2);
        LMPlayerServer p = LMPlayerServer.get(args[0]);

        if (args[1].equals("list")) return new ChatComponentText(LMStringUtils.strip(p.homes.list()));

        checkArgs(args, 3);

        BlockDimPos pos = p.homes.get(args[2]);
        if (pos == null) return error(ServerUtilities.mod.chatComponent("cmd.home_not_set", args[2]));

        if (args[1].equals("tp")) {
            LMDimUtils.teleportPlayer(getCommandSenderAsPlayer(ics), pos);
            return ServerUtilities.mod.chatComponent("cmd.warp_tp", args[2]);
        } else if (args[1].equals("remove")) {
            if (p.homes.set(args[2], null)) return ServerUtilities.mod.chatComponent("cmd.home_del", args[2]);
        }

        return error(ServerUtilities.mod.chatComponent("invalid_subcmd", args[2]));
    }
}
