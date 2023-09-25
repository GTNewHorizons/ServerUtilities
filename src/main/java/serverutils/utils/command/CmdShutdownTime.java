package serverutils.utils.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import com.feed_the_beast.ftblib.FTBLib;
import com.feed_the_beast.ftblib.lib.command.CmdBase;
import com.feed_the_beast.ftblib.lib.util.StringUtils;
import serverutils.utils.ServerUtilities;
import serverutils.utils.data.FTBUtilitiesUniverseData;

/**
 * @author LatvianModder
 */
public class CmdShutdownTime extends CmdBase {

    public CmdShutdownTime() {
        super("shutdown_time", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (FTBUtilitiesUniverseData.shutdownTime > 0L) {
            sender.addChatMessage(
                    ServerUtilities.lang(
                            sender,
                            "ftbutilities.lang.timer.shutdown",
                            StringUtils.getTimeString(
                                    FTBUtilitiesUniverseData.shutdownTime - System.currentTimeMillis())));
        } else {
            throw FTBLib.errorFeatureDisabledServer(sender);
        }
    }
}
