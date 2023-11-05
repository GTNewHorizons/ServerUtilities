package serverutils.command.client;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.lib.command.CmdBase;
import serverutils.lib.gui.GuiHelper;
import serverutils.lib.util.StringUtils;

public class CommandSimulateButton extends CmdBase {

    public CommandSimulateButton() {
        super("serverutilities_simulate_button", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        GuiHelper.BLANK_GUI.handleClick(StringUtils.joinSpaceUntilEnd(0, args));
    }
}
