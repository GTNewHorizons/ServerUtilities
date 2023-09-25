package serverutils.utils.ranks;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;

import cpw.mods.fml.common.ModContainer;
import serverutils.lib.lib.command.CmdTreeBase;
import serverutils.lib.lib.command.CommandTreeBase;

public class CommandTreeOverride extends CmdTreeBase {

    public final CommandTreeBase mirrored;

    public CommandTreeOverride(CommandTreeBase c, String parent, @Nullable ModContainer container) {
        super(c.getCommandName());
        mirrored = c;
        String node = parent + '.' + mirrored.getCommandName();

        for (ICommand command : mirrored.getSubCommands()) {
            addSubcommand(CommandOverride.create(command, node, container));
        }
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
    public boolean isUsernameIndex(String[] args, int index) {
        return mirrored.isUsernameIndex(args, index);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            mirrored.processCommand(sender, args);
        } else {
            super.processCommand(sender, args);
        }
    }
}
