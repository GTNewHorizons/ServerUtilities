package serverutils.command.pausewhenempty;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

import serverutils.ServerUtilities;
import serverutils.data.IPauseWhenEmpty;
import serverutils.lib.command.CmdBase;

public class CmdPauseWhenEmptySet extends CmdBase {

    CmdPauseWhenEmptySet() {
        super("set", Level.OP);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        checkArgs(sender, args, 1);
        int newValue = parseIntWithMin(sender, args[0], 0);

        if (MinecraftServer.getServer() instanceof IPauseWhenEmpty pauseWhenEmpty) {
            pauseWhenEmpty.serverUtilities$setPauseWhenEmptySeconds(newValue, false);
            sender.addChatMessage(ServerUtilities.lang(sender, "cmd.pause_when_empty_updated", newValue));
        }
    }
}
