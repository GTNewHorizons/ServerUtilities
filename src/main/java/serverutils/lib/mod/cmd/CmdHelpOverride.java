package serverutils.lib.mod.cmd;

import java.util.Collections;
import java.util.List;

import net.minecraft.command.*;
import net.minecraft.server.MinecraftServer;

public class CmdHelpOverride extends CommandHelp {

    public CmdHelpOverride() {
        super();
    }

    public String getCommandUsage(ICommandSender ics) {
        return '/' + getCommandName() + " [command]";
    }

    protected List<ICommand> getSortedPossibleCommands(ICommandSender ics) {
        List<ICommand> list = MinecraftServer.getServer().getCommandManager().getPossibleCommands(ics);
        try {
            Collections.sort(list);
        } catch (Exception e) {}
        return list;
    }
}
