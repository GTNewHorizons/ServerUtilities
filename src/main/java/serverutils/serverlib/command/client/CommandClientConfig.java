package serverutils.serverlib.command.client;

import serverutils.serverlib.client.GuiClientConfig;
import serverutils.serverlib.lib.command.CmdBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

public class CommandClientConfig extends CmdBase {
	public CommandClientConfig()
	{
		super("client_config", Level.ALL);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		new GuiClientConfig().openGuiLater();
	}
}