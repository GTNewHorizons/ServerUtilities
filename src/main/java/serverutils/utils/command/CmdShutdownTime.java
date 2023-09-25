package serverutils.utils.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.lib.command.CmdBase;
import serverutils.lib.lib.util.StringUtils;
import serverutils.utils.ServerUtilities;
import serverutils.utils.data.ServerUtilitiesUniverseData;

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
            throw ServerUtilitiesLib.errorFeatureDisabledServer(sender);
        }
    }
}
