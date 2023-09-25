package serverutils.lib.lib.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.command.CommandHelp;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;

public class CmdTreeHelp extends CommandHelp {

	private final CommandTreeBase parent;

	public CmdTreeHelp(CommandTreeBase parent) {
		this.parent = parent;
	}

	@Override
	public boolean canCommandSenderUseCommand(ICommandSender sender) {
		return true;
	}

	@Override
	protected List<ICommand> getSortedPossibleCommands(ICommandSender sender) {
		List<ICommand> list = new ArrayList<>();

		for (ICommand command : parent.getSubCommands()) {
			if (command.canCommandSenderUseCommand(sender)) {
				list.add(command);
			}
		}

		Collections.sort(list);
		return list;
	}

	@Override
	protected Map<String, ICommand> getCommands() {
		return parent.getCommandMap();
	}
}