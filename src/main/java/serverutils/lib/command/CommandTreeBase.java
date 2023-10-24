package serverutils.lib.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;

/**
 * Base class for commands that has subcommands.
 * <p>
 * E.g. /team settings set [value] settings is subcommand of team and set is subcommand of settings
 */
public abstract class CommandTreeBase extends CommandBase {

    private final Map<String, ICommand> commandMap = new HashMap<>();
    private final Map<String, ICommand> commandAliasMap = new HashMap<>();

    public void addSubcommand(ICommand command) {
        commandMap.put(command.getCommandName(), command);
        if (command.getCommandAliases() == null) {
            return;
        }
        for (String alias : (List<String>) command.getCommandAliases()) {
            commandAliasMap.put(alias, command);
        }
    }

    public Collection<ICommand> getSubCommands() {
        return commandMap.values();
    }

    @Nullable
    public ICommand getSubCommand(String command) {
        ICommand cmd = commandMap.get(command);
        if (cmd != null) {
            return cmd;
        }
        return commandAliasMap.get(command);
    }

    public Map<String, ICommand> getCommandMap() {
        return Collections.unmodifiableMap(commandMap);
    }

    public List<ICommand> getSortedCommandList() {
        List<ICommand> list = new ArrayList<>(getSubCommands());
        Collections.sort(list);
        return list;
    }

    private static String[] shiftArgs(@Nullable String[] s) {
        if (s == null || s.length == 0) {
            return new String[0];
        }

        String[] s1 = new String[s.length - 1];
        System.arraycopy(s, 1, s1, 0, s1.length);
        return s1;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> keys = new ArrayList<>();

            for (ICommand c : getSubCommands()) {
                if (c.canCommandSenderUseCommand(sender)) {
                    keys.add(c.getCommandName());
                }
            }

            keys.sort(null);
            return getListOfStringsFromIterableMatchingLastWord(args, keys);
        }

        ICommand cmd = getSubCommand(args[0]);

        if (cmd != null) {
            return cmd.addTabCompletionOptions(sender, shiftArgs(args));
        }

        return super.addTabCompletionOptions(sender, args);
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        if (index > 0 && args.length > 1) {
            ICommand cmd = getSubCommand(args[0]);
            if (cmd != null) {
                return cmd.isUsernameIndex(shiftArgs(args), index - 1);
            }
        }

        return false;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            String subCommandsString = getAvailableSubCommandsString(MinecraftServer.getServer(), sender);
            sender.addChatMessage(
                    new ChatComponentTranslation("commands.tree_base.available_subcommands", subCommandsString));
        } else {
            ICommand cmd = getSubCommand(args[0]);

            if (cmd == null) {
                String subCommandsString = getAvailableSubCommandsString(MinecraftServer.getServer(), sender);
                throw new CommandException(
                        "commands.tree_base.invalid_cmd.list_subcommands",
                        args[0],
                        subCommandsString);
            } else if (!cmd.canCommandSenderUseCommand(sender)) {
                throw new CommandException("commands.generic.permission");
            } else {
                cmd.processCommand(sender, shiftArgs(args));
            }
        }
    }

    private String getAvailableSubCommandsString(MinecraftServer server, ICommandSender sender) {
        Collection<String> availableCommands = new ArrayList<>();
        for (ICommand command : getSubCommands()) {
            if (command.canCommandSenderUseCommand(sender)) {
                availableCommands.add(command.getCommandName());
            }
        }
        return CommandBase.joinNiceStringFromCollection(availableCommands);
    }
}
