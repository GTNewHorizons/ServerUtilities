package serverutils.serverlib.command.client;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.serverlib.lib.command.CmdBase;
import serverutils.serverlib.lib.gui.GuiHelper;
import serverutils.serverlib.lib.util.StringUtils;

public class CommandSimulateButton extends CmdBase {

	public CommandSimulateButton() {
		super("ftblib_simulate_button", Level.ALL);
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) throws CommandException {
		GuiHelper.BLANK_GUI.handleClick(StringUtils.joinSpaceUntilEnd(0, args));
	}
}