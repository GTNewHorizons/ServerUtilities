package serverutils.command.pausewhenempty;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

import serverutils.ServerUtilities;
import serverutils.data.IPauseWhenEmptyServer;
import serverutils.lib.command.CmdBase;

public class CmdPauseWhenEmptyMask extends CmdBase {

    public CmdPauseWhenEmptyMask() {
        super("mask", Level.OP);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        checkArgs(sender, args, 1);
        int newValue = parseIntWithMin(sender, args[0], -1);

        if (MinecraftServer.getServer() instanceof IPauseWhenEmptyServer pauseWhenEmpty) {
            pauseWhenEmpty.serverUtilities$setPauseWhenEmptyMaskSeconds(newValue);
            sender.addChatMessage(ServerUtilities.lang(sender, "cmd.pause_when_empty_mask", newValue));
        }
    }
}
