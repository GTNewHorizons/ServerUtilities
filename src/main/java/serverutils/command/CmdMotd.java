package serverutils.command;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.IChatComponent;

import serverutils.ServerUtilitiesConfig;
import serverutils.lib.command.CmdBase;

/**
 * Sends the server's motd to the player.
 */
public class CmdMotd extends CmdBase {

    public CmdMotd() {
        super("motd", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        for (IChatComponent component : ServerUtilitiesConfig.login.getMOTD()) {
            sender.addChatMessage(component);
        }
    }
}
