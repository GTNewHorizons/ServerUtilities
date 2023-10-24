package serverutils.command.client;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.client.gui.GuiClientConfig;
import serverutils.lib.command.CmdBase;

public class CommandClientConfig extends CmdBase {

    public CommandClientConfig() {
        super("client_config", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        new GuiClientConfig().openGuiLater();
    }
}
