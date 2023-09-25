package serverutils.lib.command.client;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.lib.lib.command.CmdBase;
import serverutils.lib.lib.gui.GuiHelper;
import serverutils.lib.lib.util.StringUtils;

public class CommandSimulateButton extends CmdBase {

	public CommandSimulateButton() {
		super("ftblib_simulate_button", Level.ALL);
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) throws CommandException {
		GuiHelper.BLANK_GUI.handleClick(StringUtils.joinSpaceUntilEnd(0, args));
	}
}