package serverutils.utils.mod.cmd;

import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IChatComponent;

import serverutils.lib.api.cmd.*;
import serverutils.utils.mod.ServerUtilities;
import serverutils.utils.world.LMPlayerServer;

public class CmdSetHome extends CommandLM {

    public CmdSetHome() {
        super("sethome", CommandLevel.ALL);
    }

    public String getCommandUsage(ICommandSender ics) {
        return '/' + commandName + " <ID>";
    }

    public String[] getTabStrings(ICommandSender ics, String[] args, int i) throws CommandException {
        if (i == 0) return LMPlayerServer.get(ics).homes.list();
        return null;
    }

    public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
        EntityPlayerMP ep = getCommandSenderAsPlayer(ics);
        LMPlayerServer p = LMPlayerServer.get(ep);
        checkArgs(args, 1);

        int maxHomes = p.getRank().config.max_homes.getAsInt();

        if (maxHomes <= 0 || p.homes.size() >= maxHomes) {
            if (maxHomes == 0 || p.homes.get(args[0]) == null)
                return error(ServerUtilities.mod.chatComponent("cmd.home_limit"));
        }

        p.homes.set(args[0], p.getPos());
        return ServerUtilities.mod.chatComponent("cmd.home_set", args[0]);
    }
}
