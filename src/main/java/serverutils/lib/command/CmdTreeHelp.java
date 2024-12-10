package serverutils.lib.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.command.CommandHelp;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.StatCollector;

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

        list.sort(null);
        return list;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return StatCollector.translateToLocal(parent.getCommandUsage(sender)) + " help";
    }

    @Override
    protected Map<String, ICommand> getCommands() {
        return parent.getCommandMap();
    }
}
