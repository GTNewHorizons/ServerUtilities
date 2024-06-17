package serverutils.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.lib.command.CmdBase;
import serverutils.lib.data.Universe;
import serverutils.task.ShutdownTask;

public class CmdShutdown extends CmdBase {

    public CmdShutdown() {
        super("shutdown", Level.OP);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        ShutdownTask task = new ShutdownTask();
        task.execute(Universe.get());
    }
}
