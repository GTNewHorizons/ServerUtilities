package serverutils.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.ServerUtilities;
import serverutils.data.ServerUtilitiesUniverseData;
import serverutils.lib.command.CmdBase;
import serverutils.lib.util.StringUtils;

public class CmdShutdownTime extends CmdBase {

    public CmdShutdownTime() {
        super("shutdown_time", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (ServerUtilitiesUniverseData.shutdownTime > 0L) {
            sender.addChatMessage(
                    ServerUtilities.lang(
                            sender,
                            "serverutilities.lang.timer.shutdown",
                            StringUtils.getTimeString(
                                    ServerUtilitiesUniverseData.shutdownTime - System.currentTimeMillis())));
        } else {
            throw ServerUtilities.errorFeatureDisabledServer(sender);
        }
    }
}
