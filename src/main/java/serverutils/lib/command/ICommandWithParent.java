package serverutils.lib.command;

import javax.annotation.Nullable;

import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;

public interface ICommandWithParent extends ICommand {

    @Nullable
    ICommand getParent();

    void setParent(@Nullable ICommand p);

    @Override
    default String getCommandUsage(ICommandSender sender) {
        return "commands." + getCommandPath(this) + ".usage";
    }

    static String getCommandPath(ICommand command) {
        return (command instanceof ICommandWithParent cmdWithParent && cmdWithParent.getParent() != null
                ? (getCommandPath(cmdWithParent.getParent()) + ".")
                : "") + command.getCommandName();
    }
}
