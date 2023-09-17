package serverutils.utils.mod.cmd;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IChatComponent;

import serverutils.lib.LMDimUtils;
import serverutils.lib.api.cmd.CommandLM;
import serverutils.lib.api.cmd.CommandLevel;
import serverutils.utils.mod.ServerUtilities;
import serverutils.utils.world.LMPlayerServer;

public class CmdBack extends CommandLM {

    public CmdBack() {
        super("back", CommandLevel.ALL);
    }

    public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
        EntityPlayerMP ep = getCommandSenderAsPlayer(ics);
        LMPlayerServer p = LMPlayerServer.get(ep);
        if (p.lastDeath == null) return error(ServerUtilities.mod.chatComponent("cmd.no_dp"));
        LMDimUtils.teleportPlayer(ep, p.lastDeath);
        p.lastDeath = null;

        return null;
    }
}
