package serverutils.utils.mod.cmd.admin;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IChatComponent;

import serverutils.lib.api.cmd.CommandLM;
import serverutils.lib.api.cmd.CommandLevel;
import serverutils.utils.mod.cmd.InvSeeInventory;

public class CmdInvsee extends CommandLM {

    public CmdInvsee() {
        super("invsee", CommandLevel.OP);
    }

    public String getCommandUsage(ICommandSender ics) {
        return '/' + commandName + " <player>";
    }

    public Boolean getUsername(String[] args, int i) {
        return (i == 0) ? Boolean.TRUE : null;
    }

    public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
        checkArgs(args, 1);
        EntityPlayerMP ep0 = getCommandSenderAsPlayer(ics);
        EntityPlayerMP ep = getPlayer(ics, args[0]);
        ep0.displayGUIChest(new InvSeeInventory(ep));
        return null;
    }
}
