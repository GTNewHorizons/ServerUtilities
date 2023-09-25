package serverutils.old.mod.cmd;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import latmod.lib.LMStringUtils;
import serverutils.lib.BlockDimPos;
import serverutils.lib.LMDimUtils;
import serverutils.lib.api.cmd.CommandLM;
import serverutils.lib.api.cmd.CommandLevel;
import serverutils.old.mod.ServerUtilities;
import serverutils.old.world.LMPlayerServer;
import serverutils.old.world.LMWorldServer;

public class CmdWarp extends CommandLM {

    public CmdWarp() {
        super("warp", CommandLevel.ALL);
    }

    public String getCommandUsage(ICommandSender ics) {
        return '/' + commandName + " <ID>";
    }

    public String[] getTabStrings(ICommandSender ics, String[] args, int i) throws CommandException {
        if (i == 0) return LMWorldServer.inst.warps.list();
        return super.getTabStrings(ics, args, i);
    }

    public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
        checkArgs(args, 1);
        if (args[0].equals("list")) {
            String[] list = LMWorldServer.inst.warps.list();
            if (list.length == 0) return new ChatComponentText("-");
            return new ChatComponentText(LMStringUtils.strip(list));
        }

        EntityPlayerMP ep = getCommandSenderAsPlayer(ics);
        LMPlayerServer playerServer = LMPlayerServer.get(ep);
        BlockDimPos p = LMWorldServer.inst.warps.get(args[0]);
        if (p == null) return error(ServerUtilities.mod.chatComponent("cmd.warp_not_set", args[0]));
        if (p.dim != ep.dimension && !playerServer.getRank().config.cross_dim_warp.getAsBoolean())
            return error(ServerUtilities.mod.chatComponent("cmd.warp_not_same_dim", args[0]));
        LMDimUtils.teleportPlayer(ep, p);
        return ServerUtilities.mod.chatComponent("cmd.warp_tp", args[0]);
    }
}
