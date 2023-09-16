package ftb.lib.mod.cmd;

import java.util.List;

import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IChatComponent;

import ftb.lib.FTBLib;
import ftb.lib.api.cmd.CommandLM;
import ftb.lib.api.cmd.CommandLevel;
import latmod.lib.LMUtils;

public class CmdListOverride extends CommandLM {

    public CmdListOverride() {
        super("list", CommandLevel.ALL);
    }

    public String getCommandUsage(ICommandSender ics) {
        return '/' + commandName + " ['uuid']";
    }

    public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
        List<EntityPlayerMP> players = FTBLib.getAllOnlinePlayers(null);
        boolean printUUID = args.length > 0 && args[0].equals("uuid");

        FTBLib.printChat(ics, "Players currently online: [ " + players.size() + " ]");
        for (int i = 0; i < players.size(); i++) {
            EntityPlayerMP ep = players.get(i);

            if (printUUID)
                FTBLib.printChat(ics, ep.getCommandSenderName() + " :: " + LMUtils.fromUUID(ep.getUniqueID()));
            else FTBLib.printChat(ics, ep.getCommandSenderName());
        }

        return null;
    }
}
