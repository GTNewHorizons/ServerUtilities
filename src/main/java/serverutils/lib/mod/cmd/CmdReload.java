package serverutils.lib.mod.cmd;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.IChatComponent;

import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.api.cmd.CommandLM;
import serverutils.lib.api.cmd.CommandLevel;
import serverutils.lib.mod.config.ServerUtilitiesLibConfigCmdNames;

public class CmdReload extends CommandLM {

    public CmdReload() {
        super(ServerUtilitiesLibConfigCmdNames.reload.getAsString(), CommandLevel.OP);
    }

    public IChatComponent onCommand(ICommandSender ics, String[] args) {
        ServerUtilitiesLib.reload(ics, true, args.length > 0 && args[0].equalsIgnoreCase("client"));
        return null;
    }
}
