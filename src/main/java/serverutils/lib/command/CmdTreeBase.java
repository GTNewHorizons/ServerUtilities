package serverutils.lib.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;

public class CmdTreeBase extends CommandTreeBase implements ICommandWithParent {

    private final String name;
    private ICommand parent;

    public CmdTreeBase(String n) {
        name = n;
    }

    @Override
    public void addSubcommand(ICommand command) {
        super.addSubcommand(command);

        if (command instanceof ICommandWithParent cmdWithParent) {
            cmdWithParent.setParent(this);
        }
    }

    @Override
    public final String getCommandName() {
        return name;
    }

    @Override
    public int getRequiredPermissionLevel() {
        int level = 4;

        for (ICommand command : getSubCommands()) {
            if (command instanceof CommandBase cmdBase) {
                level = Math.min(level, cmdBase.getRequiredPermissionLevel());
            }
        }

        return level;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        for (ICommand command : getSubCommands()) {
            if (command.canCommandSenderUseCommand(sender)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public ICommand getParent() {
        return parent;
    }

    @Override
    public void setParent(ICommand c) {
        parent = c;
    }
}
