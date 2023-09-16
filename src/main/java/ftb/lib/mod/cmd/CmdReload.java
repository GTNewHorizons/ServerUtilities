package ftb.lib.mod.cmd;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.IChatComponent;

import ftb.lib.FTBLib;
import ftb.lib.api.cmd.CommandLM;
import ftb.lib.api.cmd.CommandLevel;
import ftb.lib.mod.config.FTBLibConfigCmdNames;

public class CmdReload extends CommandLM {

    public CmdReload() {
        super(FTBLibConfigCmdNames.reload.getAsString(), CommandLevel.OP);
    }

    public IChatComponent onCommand(ICommandSender ics, String[] args) {
        FTBLib.reload(ics, true, args.length > 0 && args[0].equalsIgnoreCase("client"));
        return null;
    }
}
