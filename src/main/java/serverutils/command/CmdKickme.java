package serverutils.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.ServerUtilities;
import serverutils.lib.command.CmdBase;

public class CmdKickme extends CmdBase {

    public CmdKickme() {
        super("kickme", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        if (player.mcServer.isDedicatedServer()) {
            getCommandSenderAsPlayer(sender).playerNetServerHandler.kickPlayerFromServer(
                    ServerUtilities.lang(sender, "serverutilities.lang.kickme").getUnformattedText());
        } else {
            player.mcServer.initiateShutdown();
        }
    }
}
