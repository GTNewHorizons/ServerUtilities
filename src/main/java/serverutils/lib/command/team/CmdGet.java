package serverutils.lib.command.team;

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import serverutils.lib.lib.command.CmdBase;
import serverutils.lib.lib.command.CommandUtils;
import serverutils.lib.lib.data.ForgePlayer;
import serverutils.lib.lib.data.Universe;

public class CmdGet extends CmdBase {

    public CmdGet() {
        super("get", Level.ALL);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return matchFromIterable(args, Universe.get().getPlayers());
        }

        return super.addTabCompletionOptions(sender, args);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        checkArgs(sender, args, 1);
        ForgePlayer player = CommandUtils.getSelfOrOther(sender, args, 0);
        IChatComponent component = new ChatComponentText("");
        component.appendSibling(player.getDisplayName());
        component.appendText(": ");
        component.appendSibling(player.team.getCommandTitle());
        sender.addChatMessage(component);
    }
}
