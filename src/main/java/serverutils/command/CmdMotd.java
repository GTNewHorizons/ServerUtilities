package serverutils.command;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

import serverutils.lib.command.CmdBase;
import serverutils.lib.util.MOTDFormatter;

/**
 * Sends the server's motd to the player.
 */
public class CmdMotd extends CmdBase {

    public CmdMotd() {
        super("motd", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        MinecraftServer server = MinecraftServer.getServer();

        if (server != null) sender.addChatMessage(MOTDFormatter.buildMOTD(server));
    }
}
