package ftb.lib.api.cmd;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.command.*;
import net.minecraft.util.*;

import latmod.lib.LMListUtils;
import latmod.lib.LMStringUtils;

public class CommandSubLM extends CommandLM implements ICustomCommandInfo {

    public final Map<String, CommandLM> subCommands;

    public CommandSubLM(String s, CommandLevel l) {
        super(s, l);
        subCommands = new HashMap<>();
    }

    public void add(CommandLM c) {
        subCommands.put(c.commandName, c);
    }

    public String getCommandUsage(ICommandSender ics) {
        return "/" + commandName + " [ " + LMStringUtils.strip(LMListUtils.toStringArray(subCommands.keySet())) + " ]";
    }

    public String[] getTabStrings(ICommandSender ics, String args[], int i) throws CommandException {
        if (i == 0) return LMListUtils.toStringArray(subCommands.keySet());

        CommandLM cmd = subCommands.get(args[0]);

        if (cmd != null) {
            String[] s = cmd.getTabStrings(ics, LMStringUtils.shiftArray(args), i - 1);
            if (s != null && s.length > 0) return s;
        }

        return super.getTabStrings(ics, args, i);
    }

    public Boolean getUsername(String[] args, int i) {
        if (i > 0 && args.length > 1) {
            CommandLM cmd = subCommands.get(args[0]);
            if (cmd != null) return cmd.getUsername(LMStringUtils.shiftArray(args), i - 1);
        }

        return null;
    }

    public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
        if (args == null || args.length == 0)
            return new ChatComponentText(LMStringUtils.strip(getTabStrings(ics, args, 0)));
        CommandLM cmd = subCommands.get(args[0]);
        if (cmd != null) return cmd.onCommand(ics, LMStringUtils.shiftArray(args));
        throw new InvalidSubCommandException(args[0]);
    }

    public void addInfo(List<IChatComponent> list, ICommandSender sender) {
        list.add(new ChatComponentText('/' + commandName));
        list.add(null);
        addCommandUsage(sender, list, 0);
    }

    private static IChatComponent tree(IChatComponent sibling, int level) {
        if (level == 0) return sibling;
        char[] chars = new char[level * 2];
        Arrays.fill(chars, ' ');
        return new ChatComponentText(new String(chars)).appendSibling(sibling);
    }

    private void addCommandUsage(ICommandSender ics, List<IChatComponent> list, int level) {
        for (CommandLM c : subCommands.values()) {
            if (c instanceof CommandSubLM) {
                list.add(tree(new ChatComponentText('/' + c.commandName), level));
                ((CommandSubLM) c).addCommandUsage(ics, list, level + 1);
            } else {
                String usage = c.getCommandUsage(ics);
                if (usage.indexOf('/') != -1 || usage.indexOf('%') != -1) {
                    list.add(tree(new ChatComponentText(usage), level));
                } else {
                    list.add(tree(new ChatComponentTranslation(usage), level));
                }
            }
        }
    }
}
