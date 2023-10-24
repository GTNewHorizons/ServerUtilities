package serverutils.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

import serverutils.lib.command.CmdBase;
import serverutils.lib.util.FileUtils;

public class CmdShutdown extends CmdBase {

    public CmdShutdown() {
        super("shutdown", Level.OP);
    }

    public static void shutdown(MinecraftServer server) {
        FileUtils.newFile(server.getFile("autostart.stamp"));
        server.initiateShutdown();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        shutdown(getCommandSenderAsPlayer(sender).mcServer);
    }
}
