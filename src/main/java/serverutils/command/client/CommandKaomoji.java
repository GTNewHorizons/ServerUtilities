package serverutils.command.client;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.lib.client.ClientUtils;
import serverutils.lib.command.CmdBase;
import serverutils.lib.util.StringJoiner;

public class CommandKaomoji extends CmdBase {

    private final String emoji;

    public CommandKaomoji(String n, String e) {
        super(n, Level.ALL);
        emoji = e;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        String txt = StringJoiner.with(' ').joinStrings(args);

        if (txt.isEmpty()) {
            ClientUtils.execClientCommand(emoji);
        } else {
            ClientUtils.execClientCommand(txt + " " + emoji);
        }
    }
}
