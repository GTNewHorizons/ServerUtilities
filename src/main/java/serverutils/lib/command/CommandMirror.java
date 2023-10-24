package serverutils.lib.command;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;

public class CommandMirror extends CommandBase {

    public final ICommand mirrored;

    public CommandMirror(ICommand c) {
        mirrored = c;
    }

    @Override
    public String getCommandName() {
        return mirrored.getCommandName();
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return mirrored.getCommandUsage(sender);
    }

    @Override
    public List<String> getCommandAliases() {
        return mirrored.getCommandAliases();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        mirrored.processCommand(sender, args);
    }

    @Override
    public int getRequiredPermissionLevel() {
        return mirrored instanceof CommandBase mirroredBase ? mirroredBase.getRequiredPermissionLevel() : 4;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return mirrored.canCommandSenderUseCommand(sender);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        return mirrored.addTabCompletionOptions(sender, args);
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return mirrored.isUsernameIndex(args, index);
    }

    @Override
    public int compareTo(ICommand o) {
        return getCommandName().compareTo(o.getCommandName());
    }
}
