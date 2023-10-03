package serverutils.utils.ranks;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.eventhandler.Event;
import serverutils.lib.lib.command.CommandTreeBase;

public class CommandOverride extends CommandBase {

    public static ICommand create(ICommand command, String parent, @Nullable ModContainer container) {
        if (command instanceof CommandTreeBase) {
            return new CommandTreeOverride((CommandTreeBase) command, parent, container);
        } else {
            return new CommandOverride(command, parent, container);
        }
    }

    public final ICommand mirrored;
    public final String node;
    // public final IChatComponent usage;
    public final ModContainer modContainer;

    private CommandOverride(ICommand c, String parent, @Nullable ModContainer container) {
        mirrored = c;
        node = parent + '.' + mirrored.getCommandName();
        Ranks.INSTANCE.commands.put(node, this);

        // TODO: Fix crash with other mods

        // String usageS = getCommandUsage(FakePlayerFactory.getMinecraft(Ranks.INSTANCE.universe.world));
        //
        // if (usageS == null || usageS.isEmpty()
        // || usageS.indexOf('/') != -1
        // || usageS.indexOf('%') != -1
        // || usageS.indexOf(' ') != -1) {
        // usage = new ChatComponentText(usageS);
        // } else {
        // usage = new ChatComponentTranslation(usageS);
        // }

        modContainer = container;
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
        return mirrored instanceof CommandBase ? ((CommandBase) mirrored).getRequiredPermissionLevel() : 4;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        if (sender instanceof EntityPlayerMP) {
            Event.Result result = Ranks.INSTANCE.getPermissionResult((EntityPlayerMP) sender, node, true);

            if (result != Event.Result.DEFAULT) {
                return result == Event.Result.ALLOW;
            }
        }

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
        return o instanceof CommandOverride ? node.compareTo(((CommandOverride) o).node)
                : getCommandName().compareTo(o.getCommandName());
    }
}
