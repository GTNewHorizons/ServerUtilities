package serverutils.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.ServerUtilities;
import serverutils.lib.command.CmdBase;
import serverutils.lib.util.StringUtils;
import serverutils.task.ShutdownTask;

public class CmdShutdownTime extends CmdBase {

    public CmdShutdownTime() {
        super("shutdown_time", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (ShutdownTask.shutdownTime > 0L) {
            sender.addChatMessage(
                    ServerUtilities.lang(
                            sender,
                            "serverutilities.lang.timer.shutdown",
                            StringUtils.getTimeString(ShutdownTask.shutdownTime - System.currentTimeMillis())));
        } else {
            throw ServerUtilities.errorFeatureDisabledServer(sender);
        }
    }
}
